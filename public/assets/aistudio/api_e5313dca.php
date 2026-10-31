<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

function out(int $status, array $data): never {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}
function pdo(): PDO {
    static $db;
    if ($db instanceof PDO) return $db;
    // Shared hosting layouts may not allow placing secrets beside the domain root.
    // Prefer the user-selected, access-restricted folder inside this deployment.
    $file = __DIR__ . '/local/config.local.php';
    if (!is_file($file)) $file = dirname(__DIR__) . '/config.local.php';
    if (!is_file($file)) out(500, ['error' => 'Sunucu yapılandırması eksik']);
    $cfg = require $file; $d = $cfg['db'];
    date_default_timezone_set($cfg['timezone'] ?? 'Europe/Istanbul');
    $db = new PDO("mysql:host={$d['host']};dbname={$d['name']};charset={$d['charset']}", $d['user'], $d['password'], [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]);
    return $db;
}
function input(): array {
    $v = json_decode(file_get_contents('php://input'), true);
    return is_array($v) ? $v : [];
}
function id(): string { return bin2hex(random_bytes(16)); }
function nowMs(): int { return (int) floor(microtime(true) * 1000); }
function sessionHash(string $session): string {
    if (!preg_match('/^[a-zA-Z0-9_-]{16,128}$/', $session)) out(422, ['error' => 'Oturum bilgisi geçersiz']);
    return hash('sha256', $session);
}
function findCatalog(PDO $db, string $key): ?array {
    if ($key === '' || strlen($key) > 255) return null;
    $q = $db->prepare("SELECT c.id,c.business_id,c.title,c.description,c.slug,c.cover_image_url,c.tags_json,c.is_active,c.view_count,c.order_count,
        b.business_name,b.phone_whatsapp,b.logo_uri,b.currency_code,b.currency_symbol,b.catalog_theme_color_hex,b.catalog_grid_columns,b.catalog_show_prices,b.min_order_amount,b.shipping_fee,b.custom_slug
        FROM catalogues c LEFT JOIN businesses b ON b.id=c.business_id
        WHERE c.is_active=1 AND (c.slug=? OR c.id=?) ORDER BY c.created_at DESC LIMIT 1");
    $q->execute([$key,$key]); $row=$q->fetch(); return $row ?: null;
}
function logVisit(PDO $db, array $catalog, string $session, string $event, ?array $product=null, int $quantity=1): void {
    $at = nowMs(); $sid = sessionHash($session); $id = id();
    $payload = ['event_type'=>$event,'catalog_id'=>$catalog['id'],'product_id'=>$product['id'] ?? null,'product_name'=>$product['name'] ?? null,'quantity'=>$quantity];
    $db->prepare('INSERT INTO visits (id,session_id,event_type,catalog_id,short_id,product_id,product_name,quantity,referrer,created_at,payload_json) VALUES (?,?,?,?,?,?,?,?,?,?,?)')
       ->execute([$id,$sid,$event,$catalog['id'],null,$product['id'] ?? null,$product['name'] ?? null,$quantity,'web',$at,json_encode($payload,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES)]);
}

try {
    $db = pdo(); $action = (string)($_GET['action'] ?? '');
    if ($action === 'catalog' && $_SERVER['REQUEST_METHOD'] === 'GET') {
        $catalog = findCatalog($db,(string)($_GET['key'] ?? ''));
        if (!$catalog) out(404,['error'=>'Katalog bulunamadı veya paylaşım dışı']);
        $q=$db->prepare('SELECT id,name,sku,description,price,original_price,currency,image_url,gallery_urls_json,category,stock_status,stock_count,in_stock,variants_json FROM products WHERE catalog_id=? AND in_stock=1 ORDER BY sort_order ASC, created_at DESC');
        $q->execute([$catalog['id']]); $products=$q->fetchAll();
        $q=$db->prepare('SELECT COUNT(*) FROM catalog_live_sessions WHERE catalog_id=? AND last_seen_at>=?'); $q->execute([$catalog['id'],nowMs()-90000]); $viewers=(int)$q->fetchColumn();
        unset($catalog['view_count'],$catalog['order_count']);
        out(200,['catalog'=>$catalog,'products'=>$products,'active_viewers'=>$viewers]);
    }
    if ($action === 'activity' && $_SERVER['REQUEST_METHOD'] === 'POST') {
        $b=input(); $catalog=findCatalog($db,(string)($b['key']??'')); if(!$catalog) out(404,['error'=>'Katalog bulunamadı']);
        $session=(string)($b['session_id']??''); $hash=sessionHash($session); $event=(string)($b['event']??'heartbeat');
        if(!in_array($event,['heartbeat','catalog_open','product_view','add_to_cart'],true)) out(422,['error'=>'Aktivite türü geçersiz']);
        $at=nowMs();
        $db->prepare('INSERT INTO catalog_live_sessions (catalog_id,session_id,last_seen_at) VALUES (?,?,?) ON DUPLICATE KEY UPDATE last_seen_at=VALUES(last_seen_at)')->execute([$catalog['id'],$hash,$at]);
        if($event!=='heartbeat') {
            $product=null;
            if(in_array($event,['product_view','add_to_cart'],true)) { $q=$db->prepare('SELECT id,name FROM products WHERE id=? AND catalog_id=?'); $q->execute([(string)($b['product_id']??''),$catalog['id']]);$product=$q->fetch();if(!$product)out(422,['error'=>'Ürün bulunamadı']); }
            logVisit($db,$catalog,$session,$event,$product,max(1,min(999,(int)($b['quantity']??1))));
            if($event==='catalog_open')$db->prepare('UPDATE catalogues SET view_count=view_count+1 WHERE id=?')->execute([$catalog['id']]);
        }
        out(200,['ok'=>true]);
    }
    if ($action === 'order' && $_SERVER['REQUEST_METHOD'] === 'POST') {
        $b=input(); $catalog=findCatalog($db,(string)($b['key']??'')); if(!$catalog)out(404,['error'=>'Katalog bulunamadı']);
        $name=trim((string)($b['name']??''));$phone=trim((string)($b['phone']??''));$address=trim((string)($b['address']??''));$note=trim((string)($b['note']??''));
        if($name===''||mb_strlen($name)>255||!preg_match('/^[+0-9() .-]{7,64}$/',$phone))out(422,['error'=>'Ad soyad ve geçerli telefon gerekli']);
        if(!is_array($b['items']??null)||count($b['items'])<1||count($b['items'])>100)out(422,['error'=>'Sepet boş veya geçersiz']);
        $quantities=[];foreach($b['items'] as $item){$pid=(string)($item['product_id']??'');$qty=(int)($item['quantity']??0);if($pid===''||strlen($pid)>128||$qty<1||$qty>999)out(422,['error'=>'Ürün veya adet geçersiz']);$quantities[$pid]=min(999,($quantities[$pid]??0)+$qty);}
        $db->beginTransaction();
        try {
            $ids=array_keys($quantities);$marks=implode(',',array_fill(0,count($ids),'?'));
            // DÜZELTME: image_url ve gallery_urls_json sorguya dahil edildi
            $q=$db->prepare("SELECT id,name,sku,price,currency,image_url,gallery_urls_json,in_stock,stock_count FROM products WHERE catalog_id=? AND in_stock=1 AND id IN ($marks) FOR UPDATE");
            $q->execute(array_merge([$catalog['id']],$ids));
            $rows=$q->fetchAll();
            if(count($rows)!==count($ids))throw new DomainException('Sepette artık satışta olmayan ürün var.');
            $currency=(string)($catalog['currency_symbol']?:$catalog['currency_code']?:'₺');$sum=0.0;$snapshot=[];
            foreach($rows as $p){
                $qty=$quantities[$p['id']];
                if((int)$p['stock_count']>0 && $qty>(int)$p['stock_count'])throw new DomainException('Ürün stok adedi yetersiz: '.$p['name']);
                $price=(float)$p['price'];
                $sum=round($sum+$price*$qty,2);
                $img=(string)($p['image_url'] ?? '');
                // DÜZELTME: Sipariş kalemine doğru fotoğraf bilgisi (image_url ve imageUrl) eklendi
                $snapshot[]=[
                    'product_id'=>$p['id'],
                    'productId'=>$p['id'],
                    'name'=>$p['name'],
                    'productName'=>$p['name'],
                    'sku'=>$p['sku'],
                    'unit_price'=>$price,
                    'unitPrice'=>$price,
                    'quantity'=>$qty,
                    'currency'=>$p['currency'],
                    'image_url'=>$img,
                    'imageUrl'=>$img
                ];
            }
            $min=(float)$catalog['min_order_amount'];if($sum<$min)throw new DomainException('Minimum sipariş tutarı '.number_format($min,2,',','.').' '.$currency.' şeklindedir.');
            $shipping=(float)$catalog['shipping_fee'];$total=round($sum+$shipping,2);$orderId=id();$orderNumber='K'.strtoupper(substr(bin2hex(random_bytes(6)),0,10));$at=nowMs();
            $timeline=[['status'=>'NEW','at'=>$at,'source'=>'web_catalog']];
            $payload=['id'=>$orderId,'order_number'=>$orderNumber,'catalog_id'=>$catalog['id'],'catalog_title'=>$catalog['title'],'business_id'=>$catalog['business_id'],'customer_name'=>$name,'customer_phone'=>$phone,'customer_address'=>$address,'customer_note'=>$note,'items'=>$snapshot,'total_amount'=>$total,'currency'=>$currency,'status'=>'NEW','created_at'=>$at,'source'=>'web_catalog'];
            $db->prepare('INSERT INTO orders (id,order_number,catalog_id,catalog_title,business_id,customer_name,customer_phone,customer_address,customer_note,items_json,total_amount,currency,status,timeline_json,stock_deducted,reseller_id,reseller_name,created_at,payload_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)')
               ->execute([$orderId,$orderNumber,$catalog['id'],$catalog['title'],$catalog['business_id'],$name,$phone,$address,$note,json_encode($snapshot,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES),$total,$currency,'NEW',json_encode($timeline),0,null,null,$at,json_encode($payload,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES)]);
            $db->prepare('UPDATE catalogues SET order_count=order_count+1 WHERE id=?')->execute([$catalog['id']]);
            $db->commit();
            out(201,['ok'=>true,'order_number'=>$orderNumber,'total_amount'=>number_format($total,2,'.',''),'currency'=>$currency,'whatsapp_phone'=>$catalog['phone_whatsapp']]);
        }catch(Throwable $e){$db->rollBack();if($e instanceof DomainException)out(422,['error'=>$e->getMessage()]);throw $e;}
    }
    out(404,['error'=>'API yolu bulunamadı']);
} catch(Throwable $e) { error_log((string)$e); out(500,['error'=>'Sunucu hatası']); }
