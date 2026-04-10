<?php
/**
 * WooCommerce Store Sample Configuration
 * SQLite database mode (offline)
 */
define('DB_ENGINE', 'sqlite');
define('DB_DIR', __DIR__ . '/wp-content/database/');
define('DB_FILE', '.ht.sqlite');
define('DB_NAME', '');
define('DB_USER', '');
define('DB_PASSWORD', '');
define('DB_HOST', '');
define('DB_CHARSET', 'utf8mb4');
define('DB_COLLATE', '');

define('AUTH_KEY', 'sample-woo-auth-key');
define('SECURE_AUTH_KEY', 'sample-woo-secure-auth');
define('LOGGED_IN_KEY', 'sample-woo-logged-in');
define('NONCE_KEY', 'sample-woo-nonce');
define('AUTH_SALT', 'sample-woo-auth-salt');
define('SECURE_AUTH_SALT', 'sample-woo-secure-salt');
define('LOGGED_IN_SALT', 'sample-woo-logged-salt');
define('NONCE_SALT', 'sample-woo-nonce-salt');

$table_prefix = 'wp_';

define('WP_AUTO_UPDATE_CORE', false);
define('AUTOMATIC_UPDATER_DISABLED', true);
define('WP_DEBUG', false);
define('FS_METHOD', 'direct');
define('DISABLE_WP_CRON', true);

if (!defined('ABSPATH')) {
    define('ABSPATH', __DIR__ . '/');
}
require_once ABSPATH . 'wp-settings.php';
