<?php
/**
 * KhelaGhor Premium Dark CSS Admin Dashboard
 * Secure MySQL PDO controls, Matches management and Adsterra Master switches.
 */

session_start();

define('ADMIN_PASSWORD', 'KhelaGhorSecureAdmin_#992026_Host'); // Change this securely for production!
define('DB_HOST', 'localhost');
define('DB_USER', 'your_database_user');
define('DB_PASS', 'your_database_password');
define('DB_NAME', 'your_database_name');

// 1. Safe Database Connection Link
$pdo = null;
$db_connected = false;
$error_msg = "";

try {
    $pdo = new PDO("mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=utf8mb4", DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES   => false,
    ]);
    $db_connected = true;
} catch (PDOException $e) {
    $error_msg = "Database Link Offline: " . htmlspecialchars($e->getMessage(), ENT_QUOTES, 'UTF-8');
}

// 2. Simple Admin Session Auth Handler
if (isset($_POST['action']) && $_POST['action'] === 'login') {
    $password = $_POST['password'] ?? '';
    if ($password === ADMIN_PASSWORD) {
        $_SESSION['authenticated'] = true;
    } else {
        $error_msg = "Invalid credentials!";
    }
}

if (isset($_GET['action']) && $_GET['action'] === 'logout') {
    $_SESSION['authenticated'] = false;
    session_destroy();
    header("Location: admin.php");
    exit();
}

$authenticated = $_SESSION['authenticated'] ?? false;

// If authenticated and database is online, process operations
if ($authenticated && $db_connected) {
    // Handle Match Creation
    if (isset($_POST['action']) && $_POST['action'] === 'add_match') {
        $title = trim($_POST['title'] ?? '');
        $team1Name = trim($_POST['team1Name'] ?? '');
        $team1Logo = trim($_POST['team1Logo'] ?? '');
        $team2Name = trim($_POST['team2Name'] ?? '');
        $team2Logo = trim($_POST['team2Logo'] ?? '');
        $category = trim($_POST['category'] ?? 'Cricket');
        $status = trim($_POST['status'] ?? 'LIVE');
        $timeText = trim($_POST['timeText'] ?? '');
        $server1Url = trim($_POST['server1Url'] ?? '');

        if ($title !== "" && $team1Name !== "" && $team2Name !== "" && $server1Url !== "") {
            $stmt = $pdo->prepare("INSERT INTO `matches` (`title`, `team1Name`, `team1Logo`, `team2Name`, `team2Logo`, `category`, `status`, `timeText`, `server1Url`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->execute([$title, $team1Name, $team1Logo, $team2Name, $team2Logo, $category, $status, $timeText, $server1Url]);
            $success_msg = "Match inserted successfully!";
        } else {
            $error_msg = "Please fill in all mandatory fields!";
        }
    }

    // Handle Match Deletion
    if (isset($_GET['action']) && $_GET['action'] === 'delete') {
        $id = (int)($_GET['id'] ?? 0);
        if ($id > 0) {
            $stmt = $pdo->prepare("DELETE FROM `matches` WHERE id = ?");
            $stmt->execute([$id]);
            $success_msg = "Match Deleted successfully!";
        }
    }

    // Handle Config/Ad Settings Mutation
    if (isset($_POST['action']) && $_POST['action'] === 'update_settings') {
        $ads_switch = trim($_POST['ads_switch'] ?? 'OFF');
        $banner_ad_script = $_POST['banner_ad_script'] ?? ''; // Keep raw to support Adsterra scripts
        $popunder_url = trim($_POST['popunder_url'] ?? '');

        $stmt = $pdo->prepare("UPDATE `settings` SET `ads_switch` = ?, `banner_ad_script` = ?, `popunder_url` = ? WHERE `id` = 1");
        $stmt->execute([$ads_switch, $banner_ad_script, $popunder_url]);
        $success_msg = "Ad Configurations updated successfully!";
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KhelaGhor - Web Control Panel</title>
    <!-- Dark themed premium styling using Bootstrap 5 and customized style blocks -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background-color: #0F172A;
            color: #F8FAFC;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        .header-banner {
            background-color: #1E293B;
            border-bottom: 3.p solid #00FF66;
            padding: 20px 0;
            margin-bottom: 30px;
        }
        .text-neon {
            color: #00FF66 !important;
            text-shadow: 0 0 10px rgba(0, 255, 102, 0.3);
        }
        .btn-neon {
            background-color: #00FF66;
            color: #0F172A;
            font-weight: bold;
            border: none;
            transition: 0.3s;
        }
        .btn-neon:hover {
            background-color: #05D957;
            transform: translateY(-2px);
            color: #0F172A;
        }
        .card-custom {
            background-color: #1E293B;
            border: 1px solid #334155;
            border-radius: 16px;
            padding: 20px;
            margin-bottom: 25px;
        }
        .table-custom {
            background-color: #1E293B;
            border-radius: 12px;
            overflow: hidden;
            border: 1px solid #334155;
        }
        .table-custom th {
            background-color: #334155 !important;
            color: #F8FAFC !important;
            border: none;
        }
        .table-custom td {
            background-color: #1E293B !important;
            color: #E2E8F0 !important;
            border-color: #334155 !important;
            vertical-align: middle;
        }
        .badge-live {
            background-color: #EF4444;
            color: white;
            font-weight: bold;
        }
        .badge-upcoming {
            background-color: #EAB308;
            color: #0F172A;
            font-weight: bold;
        }
        .form-control, .form-select {
            background-color: #0F172A;
            border: 1px solid #475569;
            color: #FFFFFF;
        }
        .form-control:focus, .form-select:focus {
            background-color: #0F172A;
            border-color: #00FF66;
            color: #FFFFFF;
            box-shadow: 0 0 0 0.25rem rgba(0, 255, 102, 0.25);
        }
    </style>
</head>
<body>

    <div class="header-banner">
        <div class="container d-flex justify-content-between align-items-center">
            <div>
                <h1 class="h3 m-0 font-weight-bold">KHELAGHOR <span class="text-neon">ADMIN PANEL</span></h1>
                <p class="small text-muted m-0">100% Custom Host Domain Dashboard (Firebase Disabled)</p>
            </div>
            <?php if ($authenticated): ?>
                <a href="?action=logout" class="btn btn-sm btn-outline-danger">Log Out</a>
            <?php endif; ?>
        </div>
    </div>

    <div class="container">
        
        <?php if (!empty($error_msg)): ?>
            <div class="alert alert-danger border-0 text-white" style="background-color: #EF4444;"><?= $error_msg ?></div>
        <?php endif; ?>

        <?php if (!empty($success_msg)): ?>
            <div class="alert alert-success border-0 text-white" style="background-color: #10B981;"><?= $success_msg ?></div>
        <?php endif; ?>

        <?php if (!$authenticated): ?>
            <!-- Login Card Layout -->
            <div class="row justify-content-center">
                <div class="col-md-5">
                    <div class="card-custom text-center">
                        <h3 class="mb-4">System Authenticator</h3>
                        <form method="POST">
                            <input type="hidden" name="action" value="login">
                            <div class="mb-3">
                                <label for="password" class="form-label text-start d-block">Admin Password</label>
                                <input type="password" name="password" id="password" class="form-control" placeholder="Enter system key" required>
                            </div>
                            <button type="submit" class="btn btn-neon w-100 py-2 mt-3">ENTER SYSTEM</button>
                        </form>
                    </div>
                </div>
            </div>
        <?php else: ?>

            <!-- Authenticated Panel Content -->
            <div class="row">
                
                <!-- Left Hand Config Form: Add Match & Ad Settings -->
                <div class="col-lg-5">
                    
                    <!-- Adsterra & Universal Ads Config Card -->
                    <div class="card-custom">
                        <h4 class="mb-4 text-neon">Unified Ad Settings</h4>
                        <?php
                        $settings = ['ads_switch' => 'OFF', 'banner_ad_script' => '', 'popunder_url' => ''];
                        if ($db_connected) {
                            $stmt = $pdo->query("SELECT * FROM `settings` LIMIT 1");
                            $settings = $stmt->fetch() ?: $settings;
                        }
                        ?>
                        <form method="POST">
                            <input type="hidden" name="action" value="update_settings">
                            
                            <div class="mb-3">
                                <label class="form-label d-block">Master Ads Switch</label>
                                <div class="form-check form-check-inline">
                                    <input class="form-check-input" type="radio" name="ads_switch" id="adsOn" value="ON" <?= $settings['ads_switch'] === 'ON' ? 'checked' : '' ?>>
                                    <label class="form-check-label" for="adsOn">ON (Ads Active)</label>
                                </div>
                                <div class="form-check form-check-inline">
                                    <input class="form-check-input" type="radio" name="ads_switch" id="adsOff" value="OFF" <?= $settings['ads_switch'] === 'OFF' ? 'checked' : '' ?>>
                                    <label class="form-check-label" for="adsOff">OFF (All Ads Off)</label>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label for="popunder_url" class="form-label">Chrome Pop-Under Browser URL</label>
                                <input type="url" name="popunder_url" id="popunder_url" class="form-control" value="<?= htmlspecialchars($settings['popunder_url'], ENT_QUOTES, 'UTF-8') ?>" placeholder="https://www.profitablecpmrate.com/....">
                                <div class="form-text text-muted">Outbound redirects to user's mobile Chrome browser in 2-3 seconds</div>
                            </div>

                            <div class="mb-3">
                                <label for="banner_ad_script" class="form-label">Native Web banner HTML Script block</label>
                                <textarea name="banner_ad_script" id="banner_ad_script" rows="4" class="form-control" placeholder='<script type="text/javascript"> ... </script>'><?= htmlspecialchars($settings['banner_ad_script'], ENT_QUOTES, 'UTF-8') ?></textarea>
                            </div>

                            <button type="submit" class="btn btn-neon w-100">SAVE AD CONFIGS</button>
                        </form>
                    </div>

                    <!-- Add New Stream Match Card -->
                    <div class="card-custom">
                        <h4 class="mb-4 text-neon">Create New Match Source</h4>
                        <form method="POST">
                            <input type="hidden" name="action" value="add_match">
                            
                            <div class="mb-3">
                                <label for="title" class="form-label">Tournament Header Title *</label>
                                <input type="text" name="title" id="title" class="form-control" placeholder="e.g. ICC World Cup 2026 Live" required>
                            </div>

                            <div class="row">
                                <div class="col-6 mb-3">
                                    <label for="team1Name" class="form-label">Team 1 Name *</label>
                                    <input type="text" name="team1Name" id="team1Name" class="form-control" placeholder="Bangladesh" required>
                                </div>
                                <div class="col-6 mb-3">
                                    <label for="team2Name" class="form-label">Team 2 Name *</label>
                                    <input type="text" name="team2Name" id="team2Name" class="form-control" placeholder="India" required>
                                </div>
                            </div>

                            <div class="row">
                                <div class="col-6 mb-3">
                                    <label for="team1Logo" class="form-label">Team 1 Logo URL</label>
                                    <input type="url" name="team1Logo" id="team1Logo" class="form-control" placeholder="https://image-url">
                                </div>
                                <div class="col-6 mb-3">
                                    <label for="team2Logo" class="form-label">Team 2 Logo URL</label>
                                    <input type="url" name="team2Logo" id="team2Logo" class="form-control" placeholder="https://image-url">
                                </div>
                            </div>

                            <div class="row">
                                <div class="col-6 mb-3">
                                    <label for="category" class="form-label">Sport Category *</label>
                                    <select name="category" id="category" class="form-select">
                                        <option value="Cricket">Cricket</option>
                                        <option value="Football">Football</option>
                                        <option value="Tennis">Tennis</option>
                                        <option value="Basketball">Basketball</option>
                                    </select>
                                </div>
                                <div class="col-6 mb-3">
                                    <label for="status" class="form-label">State Status *</label>
                                    <select name="status" id="status" class="form-select">
                                        <option value="LIVE">LIVE</option>
                                        <option value="UPCOMING">UPCOMING</option>
                                        <option value="HIGHLIGHT">HIGHLIGHT</option>
                                    </select>
                                </div>
                            </div>

                            <div class="mb-3">
                                <label for="timeText" class="form-label">Time Text Details</label>
                                <input type="text" name="timeText" id="timeText" class="form-control" placeholder="e.g. In progress, Starts in 50m">
                            </div>

                            <div class="mb-3">
                                <label for="server1Url" class="form-label">HLS Stream server Link (.m3u8) *</label>
                                <input type="url" name="server1Url" id="server1Url" class="form-control" placeholder="https://stream-provider.m3u8" required>
                            </div>

                            <button type="submit" class="btn btn-neon w-100">DEPLOY STREAM MATCH</button>
                        </form>
                    </div>

                </div>

                <!-- Right Hand Match Records Table -->
                <div class="col-lg-7">
                    <div class="card-custom">
                        <h4 class="mb-4 text-neon">Active Streams (Live Directory)</h4>
                        
                        <?php
                        $matches_list = [];
                        if ($db_connected) {
                            $stmt = $pdo->query("SELECT * FROM `matches` ORDER BY `id` DESC");
                            $matches_list = $stmt->fetchAll();
                        }
                        ?>

                        <?php if (empty($matches_list)): ?>
                            <p class="text-muted">No matches deployed yet. Create your first live source source above.</p>
                        <?php else: ?>
                            <div class="table-responsive">
                                <table class="table table-custom m-0">
                                    <thead>
                                        <tr>
                                            <th>Match Details</th>
                                            <th class="text-center">Status</th>
                                            <th class="text-end">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php foreach ($matches_list as $m): ?>
                                            <tr>
                                                <td>
                                                    <div class="fw-bold text-white"><?= htmlspecialchars($m['team1Name'], ENT_QUOTES, 'UTF-8') ?> vs <?= htmlspecialchars($m['team2Name'], ENT_QUOTES, 'UTF-8') ?></div>
                                                    <div class="small text-muted"><?= htmlspecialchars($m['title'], ENT_QUOTES, 'UTF-8') ?> (<?= htmlspecialchars($m['category'], ENT_QUOTES, 'UTF-8') ?>)</div>
                                                    <div class="small text-neon" style="font-size: 11px; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;"><?= htmlspecialchars($m['server1Url'], ENT_QUOTES, 'UTF-8') ?></div>
                                                </td>
                                                <td class="text-center">
                                                    <?php if ($m['status'] === 'LIVE'): ?>
                                                        <span class="badge badge-live">LIVE</span>
                                                    <?php else: ?>
                                                        <span class="badge badge-upcoming"><?= htmlspecialchars($m['status'], ENT_QUOTES, 'UTF-8') ?></span>
                                                    <?php endif; ?>
                                                </td>
                                                <td class="text-end">
                                                    <a href="?action=delete&id=<?= $m['id'] ?>" class="btn btn-sm btn-danger py-1 px-2" onclick="return confirm('Are you sure you want to remove this match stream?');">X</a>
                                                </td>
                                            </tr>
                                        <?php endforeach; ?>
                                    </tbody>
                                </table>
                            </div>
                        <?php endif; ?>

                    </div>
                </div>

            </div>

        <?php endif; ?>

    </div>

    <!-- Bootstrap Bundle JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
