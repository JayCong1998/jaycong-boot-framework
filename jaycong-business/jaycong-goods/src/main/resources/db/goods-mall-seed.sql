-- 前端默认商品初始化数据，来源：mall-web/src/data/products.ts。
-- 仅供手工执行，请先选择目标数据库；不由应用启动自动执行。
-- 固定 ID 为手工初始化预留值；执行前确认 ID 和 goods_code 未被占用。
-- 使用普通 INSERT，冲突时报错；不覆盖、删除或修改已有商品。重复执行会报唯一键冲突。
-- 售价由前端的元转换为数据库的分；库存无来源，设为 0；status=1 表示上架。
-- subtitle 和 description 均保留前端 description；image 映射为 cover_url。
-- cover_url 是前端静态资源相对路径，需要由商城前端所在站点提供 /images/ 资源。
-- goods 表没有 category、originalPrice、badge 字段，这些值仅记录于以下注释。

START TRANSACTION;

-- 202611110001 / MALL-EARBUDS / 无线蓝牙耳机：category=digital，原价=399元，badge=人气好物。
-- 202611110002 / MALL-PHONE / 智能手机 128G：category=digital，原价=4599元，badge=无。
-- 202611110003 / MALL-PURIFIER / 空气净化器：category=appliances，原价=1199元，badge=无。
-- 202611110004 / MALL-WATCH / 智能手表：category=digital，原价=1599元，badge=精选推荐。
-- 202611110005 / MALL-HEADPHONES / 头戴式耳机：category=digital，原价=799元，badge=无。
-- 202611110006 / MALL-LAPTOP / 轻薄办公笔记本：category=office，原价=5999元，badge=无。

INSERT INTO goods (
    id, goods_code, name, subtitle, cover_url, description, price, stock, status,
    create_time, update_time, create_user_id, update_user_id, deleted, lock_version
) VALUES
    (202611110001, 'MALL-EARBUDS', '无线蓝牙耳机', '轻盈入耳，让好声音随时相伴', '/images/earbuds.png', '轻盈入耳，让好声音随时相伴', 29900, 0, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0),
    (202611110002, 'MALL-PHONE', '智能手机 128G', '流畅体验，记录生活每一刻', '/images/phone.png', '流畅体验，记录生活每一刻', 399900, 0, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0),
    (202611110003, 'MALL-PURIFIER', '空气净化器', '静享清新，给家一次深呼吸', '/images/purifier.png', '静享清新，给家一次深呼吸', 89900, 0, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0),
    (202611110004, 'MALL-WATCH', '智能手表', '腕间好伙伴，健康时刻在线', '/images/watch.png', '腕间好伙伴，健康时刻在线', 129900, 0, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0),
    (202611110005, 'MALL-HEADPHONES', '头戴式耳机', '沉浸聆听，享受自己的时光', '/images/headphones.png', '沉浸聆听，享受自己的时光', 59900, 0, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0),
    (202611110006, 'MALL-LAPTOP', '轻薄办公笔记本', '轻装上阵，高效工作每一天', '/images/hero.png', '轻装上阵，高效工作每一天', 529900, 0, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), 0, 0, 0, 0);

COMMIT;

-- 执行后核对：应返回 6 条记录；price 单位为分。
SELECT id, goods_code, name, price, stock, status, cover_url
FROM goods WHERE goods_code IN ('MALL-EARBUDS', 'MALL-PHONE', 'MALL-PURIFIER', 'MALL-WATCH', 'MALL-HEADPHONES', 'MALL-LAPTOP')
ORDER BY id;
