// ========== API 配置 ==========
const API_BASE_URL = '/api/admin';
let apiKey = localStorage.getItem('apiKey');

// ========== 页面初始化 ==========
document.addEventListener('DOMContentLoaded', function() {
    // 检查登录状态
    if (!apiKey) {
        redirect2Login();
        return;
    }

    // 初始化事件监听
    setupTabNavigation();
    
    // 加载仪表板
    loadDashboard();
});

// ========== 选项卡导航 ==========
function setupTabNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    const tabContents = document.querySelectorAll('.tab-content');

    navItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            const tabName = this.dataset.tab;

            // 移除所有活跃类
            navItems.forEach(nav => nav.classList.remove('active'));
            tabContents.forEach(tab => tab.classList.remove('active'));

            // 添加活跃类
            this.classList.add('active');
            document.getElementById(tabName).classList.add('active');

            // 更新页面标题
            document.getElementById('page-title').textContent = this.textContent.trim();

            // 加载对应的数据
            loadTabData(tabName);
        });
    });
}

function loadTabData(tabName) {
    switch(tabName) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'apikeys':
            loadAPIKeys();
            break;
        case 'statistics':
            loadStatistics();
            break;
        case 'logs':
            loadLogs();
            break;
    }
}

// ========== 仪表板 ==========
function loadDashboard() {
    fetchAPI('/admin/health', {
        method: 'GET'
    }).then(data => {
        // 加载 API Key 统计
        return fetchAPI('/admin/api-keys/stats', { method: 'GET' });
    }).then(stats => {
        document.getElementById('total-keys').textContent = stats.total || 0;
        document.getElementById('active-keys').textContent = stats.active || 0;
        document.getElementById('revoked-keys').textContent = stats.revoked || 0;

        // 加载应用统计
        return fetchAPI('/admin/statistics', { method: 'GET' });
    }).then(data => {
        if (data.total) {
            document.getElementById('total-activations').textContent = data.total.total_activations || 0;
            document.getElementById('success-verifications').textContent = data.total.successful_verifications || 0;
            document.getElementById('total-devices').textContent = data.total.total_devices || 0;
        }

        // 加载排名前 5 的应用
        return fetchAPI('/admin/statistics/dashboard', { method: 'GET' });
    }).then(data => {
        if (data.top_apps && data.top_apps.length > 0) {
            const appsList = document.getElementById('top-apps-list');
            appsList.innerHTML = '';
            data.top_apps.forEach(app => {
                const item = document.createElement('div');
                item.className = 'app-item';
                item.innerHTML = `
                    <span class="app-name">${app.app_id}</span>
                    <span class="app-count">${app.total_activations}</span>
                `;
                appsList.appendChild(item);
            });
        }

        // 初始化图表
        initActivationChart();
    }).catch(error => {
        console.error('加载仪表板失败:', error);
        showAlert('加载仪表板失败: ' + error.message, 'danger');
    });
}

function initActivationChart() {
    const ctx = document.getElementById('activationChart');
    if (ctx) {
        const data = {
            labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
            datasets: [{
                label: '激活数',
                data: [12, 19, 3, 5, 2, 3, 9],
                borderColor: 'rgb(59, 130, 246)',
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                tension: 0.1,
                fill: true
            }]
        };

        new Chart(ctx, {
            type: 'line',
            data: data,
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
}

// ========== API Key 管理 ==========
function loadAPIKeys() {
    fetchAPI('/admin/api-keys', {
        method: 'GET'
    }).then(data => {
        const tbody = document.getElementById('apikeys-table');
        tbody.innerHTML = '';

        if (data.data && data.data.length > 0) {
            data.data.forEach(key => {
                const row = document.createElement('tr');
                const statusClass = `status-badge ${key.status}`;
                const lastUsed = key.last_used ? new Date(key.last_used).toLocaleString('zh-CN') : '未使用';
                
                row.innerHTML = `
                    <td>${key.name}</td>
                    <td><code>${key.key_prefix}</code></td>
                    <td><span class="${statusClass}">${getStatusText(key.status)}</span></td>
                    <td>${key.permission || '无'}</td>
                    <td>${lastUsed}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="editAPIKey(${key.id})">编辑</button>
                        <button class="btn btn-danger btn-sm" onclick="revokeAPIKey(${key.id})">撤销</button>
                    </td>
                `;
                tbody.appendChild(row);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 40px;">暂无 API Key</td></tr>';
        }
    }).catch(error => {
        console.error('加载 API Key 列表失败:', error);
        showAlert('加载 API Key 列表失败: ' + error.message, 'danger');
    });
}

function openGenerateModal() {
    document.getElementById('generate-modal').classList.add('show');
}

function closeGenerateModal() {
    document.getElementById('generate-modal').classList.remove('show');
    document.getElementById('key-name').value = '';
    document.querySelectorAll('.permissions-list input[type="checkbox"]').forEach(cb => cb.checked = false);
}

function generateAPIKey() {
    const name = document.getElementById('key-name').value.trim();
    if (!name) {
        showAlert('请输入 Key 名称', 'warning');
        return;
    }

    const permissions = Array.from(document.querySelectorAll('.permissions-list input[type="checkbox"]:checked'))
        .map(cb => cb.value);

    fetchAPI('/admin/api-keys', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            name: name,
            permissions: permissions
        })
    }).then(data => {
        closeGenerateModal();
        document.getElementById('new-api-key').value = data.full_key;
        document.getElementById('key-display-modal').classList.add('show');
        loadAPIKeys(); // 刷新列表
    }).catch(error => {
        console.error('生成 API Key 失败:', error);
        showAlert('生成 API Key 失败: ' + error.message, 'danger');
    });
}

function copyAPIKey() {
    const apiKeyInput = document.getElementById('new-api-key');
    apiKeyInput.select();
    document.execCommand('copy');
    showAlert('已复制到剪贴板', 'success');
}

function closeKeyDisplayModal() {
    document.getElementById('key-display-modal').classList.remove('show');
}

function revokeAPIKey(id) {
    if (!confirm('确定要撤销这个 API Key 吗？')) return;

    fetchAPI(`/admin/api-keys/${id}`, {
        method: 'DELETE'
    }).then(data => {
        showAlert('API Key 已撤销', 'success');
        loadAPIKeys();
    }).catch(error => {
        console.error('撤销 API Key 失败:', error);
        showAlert('撤销 API Key 失败: ' + error.message, 'danger');
    });
}

function editAPIKey(id) {
    showAlert('编辑功能开发中...', 'info');
}

// ========== 统计分析 ==========
function loadStatistics() {
    const appFilter = document.getElementById('app-filter')?.value || '';
    const periodFilter = parseInt(document.getElementById('period-filter')?.value || '7');

    fetchAPI('/admin/statistics', {
        method: 'GET'
    }).then(data => {
        if (data.total) {
            renderStatsTable(data.total);
        }

        // 加载趋势数据用于图表
        if (data.top_apps && data.top_apps.length > 0) {
            return fetchAPI(`/admin/statistics/apps/${data.top_apps[0].app_id}/trends?days=${periodFilter}`, 
                { method: 'GET' });
        }
    }).then(trendData => {
        if (trendData && trendData.data) {
            initStatsCharts(trendData.data);
        }
    }).catch(error => {
        console.error('加载统计数据失败:', error);
        showAlert('加载统计数据失败: ' + error.message, 'danger');
    });
}

function renderStatsTable(stats) {
    const tbody = document.getElementById('stats-table');
    tbody.innerHTML = '';

    const row = document.createElement('tr');
    row.innerHTML = `
        <td>${stats.app_id || '总计'}</td>
        <td>${stats.total_activations || 0}</td>
        <td>${stats.successful_verifications || 0}</td>
        <td>${stats.failed_verifications || 0}</td>
        <td>${stats.total_devices || 0}</td>
        <td>
            <button class="btn btn-secondary btn-sm" onclick="viewAppStats('${stats.app_id}')">详情</button>
        </td>
    `;
    tbody.appendChild(row);
}

function initStatsCharts(data) {
    // 初始化图表 1：激活统计
    const ctx1 = document.getElementById('statsChart1');
    if (ctx1) {
        new Chart(ctx1, {
            type: 'bar',
            data: {
                labels: data.map(d => new Date(d.date).toLocaleDateString('zh-CN')),
                datasets: [{
                    label: '验证数',
                    data: data.map(d => d.verification_count || 0),
                    backgroundColor: 'rgba(59, 130, 246, 0.8)'
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    // 初始化图表 2：成功率
    const ctx2 = document.getElementById('statsChart2');
    if (ctx2) {
        const successData = data.map(d => d.success_count || 0);
        const failureData = data.map(d => d.failure_count || 0);
        
        new Chart(ctx2, {
            type: 'doughnut',
            data: {
                labels: ['成功', '失败'],
                datasets: [{
                    data: [
                        successData.reduce((a, b) => a + b, 0),
                        failureData.reduce((a, b) => a + b, 0)
                    ],
                    backgroundColor: [
                        'rgba(16, 185, 129, 0.8)',
                        'rgba(239, 68, 68, 0.8)'
                    ]
                }]
            },
            options: {
                responsive: true
            }
        });
    }

    // 初始化图表 3：设备趋势
    const ctx3 = document.getElementById('statsChart3');
    if (ctx3) {
        new Chart(ctx3, {
            type: 'line',
            data: {
                labels: data.map(d => new Date(d.date).toLocaleDateString('zh-CN')),
                datasets: [{
                    label: '新设备',
                    data: data.map(d => d.new_devices || 0),
                    borderColor: 'rgb(245, 158, 11)',
                    backgroundColor: 'rgba(245, 158, 11, 0.1)',
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
}

function exportStatistics() {
    showAlert('导出功能开发中...', 'info');
}

function viewAppStats(appId) {
    showAlert(`正在查看应用 ${appId} 的详情...`, 'info');
}

// ========== 审计日志 ==========
function loadLogs() {
    const filterText = document.getElementById('log-filter')?.value || '';
    const filterType = document.getElementById('log-type-filter')?.value || '';

    fetchAPI('/admin/logs', {
        method: 'GET'
    }).then(data => {
        const tbody = document.getElementById('logs-table');
        tbody.innerHTML = '';

        if (data && data.length > 0) {
            data.forEach(log => {
                const row = document.createElement('tr');
                const timestamp = new Date(log.timestamp).toLocaleString('zh-CN');
                
                row.innerHTML = `
                    <td>${timestamp}</td>
                    <td>${log.action || '未知'}</td>
                    <td>${log.resource || '无'}</td>
                    <td>${log.ip_address || '无'}</td>
                    <td>
                        <span class="status-badge ${log.status === 'success' ? 'active' : 'inactive'}">
                            ${log.status === 'success' ? '成功' : '失败'}
                        </span>
                    </td>
                `;
                tbody.appendChild(row);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; padding: 40px;">暂无日志</td></tr>';
        }
    }).catch(error => {
        console.error('加载日志失败:', error);
        showAlert('加载日志失败: ' + error.message, 'danger');
    });
}

// ========== 工具函数 ==========
function getStatusText(status) {
    const statusMap = {
        'active': '活跃',
        'inactive': '停用',
        'revoked': '已撤销'
    };
    return statusMap[status] || status;
}

function fetchAPI(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const headers = options.headers || {};
    
    if (apiKey) {
        headers['Authorization'] = `Bearer ${apiKey}`;
    }

    return fetch(url, {
        ...options,
        headers: {
            ...headers,
            'Content-Type': 'application/json'
        }
    }).then(response => {
        if (!response.ok) {
            if (response.status === 401) {
                logout();
            }
            throw new Error(`HTTP ${response.status}`);
        }
        return response.json();
    });
}

function showAlert(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    
    // 添加到顶部
    const content = document.querySelector('.content');
    if (content) {
        content.insertBefore(alertDiv, content.firstChild);
        
        // 3 秒后自动移除
        setTimeout(() => {
            alertDiv.remove();
        }, 3000);
    }
}

function logout() {
    localStorage.removeItem('apiKey');
    redirect2Login();
}

function redirect2Login() {
    window.location.href = '/login.html';
}

function saveSettings() {
    showAlert('设置已保存', 'success');
}

function toggleAPIKeyVisibility() {
    const input = document.getElementById('current-api-key');
    if (input.type === 'password') {
        input.type = 'text';
    } else {
        input.type = 'password';
    }
}

function resetAPIKey() {
    if (!confirm('确定要重置 API Key 吗？这将使当前的 Key 失效。')) return;
    showAlert('API Key 已重置', 'success');
}

// 为 btn-sm 添加样式
const style = document.createElement('style');
style.innerHTML = `
    .btn-sm {
        padding: 6px 12px;
        font-size: 12px;
        margin-right: 5px;
    }
`;
document.head.appendChild(style);
