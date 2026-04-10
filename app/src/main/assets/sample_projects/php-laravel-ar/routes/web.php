<?php
/**
 * 路由定义 — 模仿 Laravel Route 风格
 * 
 * 格式: [HTTP方法, 路径模式, 处理器名称]
 * 路径支持 {param} 参数占位符
 */
return [
    ['GET',  '/',            'home'],
    ['GET',  '/post/{id}',   'show'],
    ['GET',  '/create',      'create'],
    ['POST', '/store',       'store'],
    ['GET',  '/delete/{id}', 'delete'],
    ['GET',  '/api/posts',   'apiPosts'],
];
