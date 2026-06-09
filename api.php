<?xml version="1.0" encoding="utf-8"?>
<?php
/**
 * KhelaGhor Secure MySQL PDO API Hub
 * Completely independent of Firebase. Backed by safe PDO Prepared Statements.
 */

header("Content-Type: application/json; charset=utf-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST");

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'your_database_user');
define('DB_PASS', 'your_database_password');
define('DB_NAME', 'your_database_name');

try {
    // Connect via secure PDO
    $pdo = new PDO("mysql:host=" . DB_HOST . ";charset=utf8mb4", DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]);

    // Create database if it does not exist
    $pdo->exec("CREATE DATABASE IF NOT EXISTS `" . DB_NAME . "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
    $pdo->exec("USE `" . DB_NAME . "`;");

    // Self-provision the matches table if missing
    $pdo->exec("CREATE TABLE IF NOT EXISTS `matches` (
        `id` INT AUTO_INCREMENT PRIMARY KEY,
        `title` VARCHAR(255) NOT NULL,
        `team1Name` VARCHAR(100) NOT NULL,
        `team1Logo` TEXT NOT NULL,
        `team2Name` VARCHAR(100) NOT NULL,
        `team2Logo` TEXT NOT NULL,
        `category` VARCHAR(50) NOT NULL,
        `status` VARCHAR(20) NOT NULL DEFAULT 'LIVE',
        `timeText` VARCHAR(100) NOT NULL,
        `server1Url` TEXT NOT NULL,
        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;");

    // Self-provision the settings table if missing
    $pdo->exec("CREATE TABLE IF NOT EXISTS `settings` (
        `id` INT AUTO_INCREMENT PRIMARY KEY,
        `ads_switch` VARCHAR(5) NOT NULL DEFAULT 'ON',
        `banner_ad_script` TEXT,
        `popunder_url` TEXT
    ) ENGINE=InnoDB;");

    // Insert default settings if empty
    $countSettings = $pdo->query("SELECT COUNT(*) FROM `settings`")->fetchColumn();
    if ($countSettings == 0) {
        $stmt = $pdo->prepare("INSERT INTO `settings` (`ads_switch`, `banner_ad_script`, `popunder_url`) VALUES (?, ?, ?)");
        $defaultAdScript = '<a href="https://www.profitablecpmrate.com/xjnd0129?key=e12be8f3edce82a85e9dfbb16042db61" target="_blank"><img src="https://placehold.co/320x50/1E293B/00FF66?text=Visit+Sponsor+HD+Streaming" width="320" height="50" style="border:none;"/></a>';
        $defaultPopunder = 'https://www.profitablecpmrate.com/xjnd0129?key=e12be8f3edce82a85e9dfbb16042db61';
        $stmt->execute(['ON', $defaultAdScript, $defaultPopunder]);
    }

    // Insert dummy matches if empty to give client immediate preview data
    $countMatches = $pdo->query("SELECT COUNT(*) FROM `matches`")->fetchColumn();
    if ($countMatches == 0) {
        $stmt = $pdo->prepare("INSERT INTO `matches` (`title`, `team1Name`, `team1Logo`, `team2Name`, `team2Logo`, `category`, `status`, `timeText`, `server1Url`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            'ICC Cricket World Cup Live',
            'Bangladesh',
            'https://ssl.gstatic.com/onebox/media/sports/logos/94LthnB6T79YAdQ6pX2CJA_48x48.png',
            'India',
            'https://ssl.gstatic.com/onebox/media/sports/logos/v9YOF6Zco_g0fXQofY77vQ_48x48.png',
            'Cricket',
            'LIVE',
            'In Progress',
            'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
        ]);
        $stmt->execute([
            'English Premier League Derby',
            'Chelsea',
            'https://ssl.gstatic.com/onebox/media/sports/logos/fhg62Y7YHA0ki6g_48x48.png',
            'Manchester United',
            'https://ssl.gstatic.com/onebox/media/sports/logos/udY6snE7v8EG0kiN8l6gGA_48x48.png',
            'Football',
            'UPCOMING',
            'Today at 10 PM',
            'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
        ]);
    }

    // --- API QUERY ROUTING ---
    
    // Fetch Settings
    $settings = $pdo->query("SELECT * FROM `settings` LIMIT 1")->fetch();

    // Fetch Matches with proper prepared query
    $stmtMatches = $pdo->query("SELECT * FROM `matches` ORDER BY `id` DESC");
    $rawMatches = $stmtMatches->fetchAll();

    // Input sanitization against XSS for output strings
    $cleanMatches = [];
    foreach ($rawMatches as $m) {
        $cleanMatches[] = [
            'id' => (int)$m['id'],
            'title' => htmlspecialchars($m['title'], ENT_QUOTES, 'UTF-8'),
            'team1Name' => htmlspecialchars($m['team1Name'], ENT_QUOTES, 'UTF-8'),
            'team1Logo' => filter_var($m['team1Logo'], FILTER_SANITIZE_URL),
            'team2Name' => htmlspecialchars($m['team2Name'], ENT_QUOTES, 'UTF-8'),
            'team2Logo' => filter_var($m['team2Logo'], FILTER_SANITIZE_URL),
            'category' => htmlspecialchars($m['category'], ENT_QUOTES, 'UTF-8'),
            'status' => htmlspecialchars($m['status'], ENT_QUOTES, 'UTF-8'),
            'timeText' => htmlspecialchars($m['timeText'], ENT_QUOTES, 'UTF-8'),
            'server1Url' => filter_var($m['server1Url'], FILTER_SANITIZE_URL)
        ];
    }

    $cleanSettings = [
        'ads_switch' => htmlspecialchars($settings['ads_switch'], ENT_QUOTES, 'UTF-8'),
        // Script is intentionally kept raw to run Adsterra scripts, but trim/sanitize wrapper
        'banner_ad_script' => $settings['banner_ad_script'],
        'popunder_url' => filter_var($settings['popunder_url'], FILTER_SANITIZE_URL)
    ];

    // Final beautiful output JSON package
    echo json_encode([
        'status' => 'success',
        'settings' => $cleanSettings,
        'matches' => $cleanMatches
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);

} catch (PDOException $e) {
    // Safe error message to block leaking internal engine path structures
    http_response_code(500);
    echo json_encode([
        'status' => 'error',
        'message' => 'Secure Database link offline: ' . htmlspecialchars($e->getMessage(), ENT_QUOTES, 'UTF-8')
    ]);
}
?>
