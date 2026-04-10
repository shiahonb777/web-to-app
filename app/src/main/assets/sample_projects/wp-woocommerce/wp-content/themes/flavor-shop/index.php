<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的商店 - WooCommerce 示例</title>
    <link rel="stylesheet" href="<?php echo get_stylesheet_uri(); ?>">
</head>
<body>
    <header class="shop-header">
        <h1>🛒 我的商店</h1>
        <p>WooCommerce 电商示例 · 由 WebToApp 创建</p>
    </header>
    <main class="products-grid">
        <div class="product-card">
            <div class="product-image">👕</div>
            <div class="product-info">
                <h3>经典T恤</h3>
                <p class="product-price">¥99.00</p>
                <p>舒适纯棉面料，经典版型设计</p>
            </div>
            <button class="add-to-cart">加入购物车</button>
        </div>
        <div class="product-card">
            <div class="product-image">👟</div>
            <div class="product-info">
                <h3>运动鞋</h3>
                <p class="product-price">¥299.00</p>
                <p>轻便透气，适合日常跑步</p>
            </div>
            <button class="add-to-cart">加入购物车</button>
        </div>
        <div class="product-card">
            <div class="product-image">🎒</div>
            <div class="product-info">
                <h3>双肩背包</h3>
                <p class="product-price">¥199.00</p>
                <p>大容量防水设计，商务出行必备</p>
            </div>
            <button class="add-to-cart">加入购物车</button>
        </div>
    </main>
</body>
</html>
