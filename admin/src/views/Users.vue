<template>
  <div class="slide-up">
    <div class="page-header">
      <div>
        <h1>用户管理</h1>
        <p class="text-secondary text-sm">管理平台所有用户和订阅</p>
      </div>
      <span class="badge badge-secondary">{{ total }} 位用户</span>
    </div>

    <!-- Filters -->
    <div class="filter-row">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input v-model="search" class="input search-input" placeholder="搜索邮箱或用户名" @input="debouncedLoad" />
      </div>
      <select v-model="planFilter" class="select" style="width:150px" @change="loadUsers">
        <option value="">所有套餐</option>
        <option value="free">Free</option>
        <option value="pro_monthly">Pro 月付</option>
        <option value="pro_yearly">Pro 年付</option>
        <option value="ultra_monthly">Ultra 月付</option>
        <option value="ultra_yearly">Ultra 年付</option>
        <option value="lifetime">终身</option>
      </select>
      <select v-model="statusFilter" class="select" style="width:110px" @change="loadUsers">
        <option value="">全部状态</option>
        <option value="active">正常</option>
        <option value="banned">封禁</option>
      </select>
    </div>

    <!-- Table -->
    <div class="card mt-16">
      <div v-if="loading" class="spinner"></div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr>
            <th>用户</th><th>密码</th><th>在线时长</th><th>套餐</th><th>到期</th><th>状态</th><th style="width:70px"></th>
          </tr></thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" class="user-row">
              <td>
                <div class="user-cell">
                  <div class="avatar">{{ (u.username || u.email)[0].toUpperCase() }}</div>
                  <div>
                    <div class="user-name">{{ u.username }}</div>
                    <div class="text-xs text-muted">{{ u.email }}</div>
                    <div v-if="u.google_email && u.google_email !== u.email" class="text-xs" style="color: #4285F4;">
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="currentColor" style="vertical-align: middle; margin-right: 2px;"><path d="M12.545,10.239v3.821h5.445c-0.712,2.315-2.647,3.972-5.445,3.972c-3.332,0-6.033-2.701-6.033-6.032s2.701-6.032,6.033-6.032c1.498,0,2.866,0.549,3.921,1.453l2.814-2.814C17.503,2.988,15.139,2,12.545,2C7.021,2,2.543,6.477,2.543,12s4.478,10,10.002,10c8.396,0,10.249-7.85,9.426-11.748L12.545,10.239z"/></svg>
                      {{ u.google_email }}
                    </div>
                  </div>
                </div>
              </td>
              <td>
                <div class="password-cell" v-if="u.password && u.password !== '—'">
                  <code v-if="u._showPw" class="pw-text">{{ u.password }}</code>
                  <span v-else class="pw-masked">••••••</span>
                  <button class="btn-icon-tiny" @click="togglePw(u)" :title="u._showPw ? '隐藏' : '显示'">
                    <svg v-if="u._showPw" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  </button>
                </div>
                <span v-else class="text-xs text-muted" title="用户登录后密码将自动记录">待登录采集</span>
              </td>
              <td class="text-sm">{{ fmtDuration(u.total_online_seconds) }}</td>
              <td><span :class="['badge', planColor(u.pro_plan)]">{{ planName(u.pro_plan) }}</span></td>
              <td class="text-sm">
                <span v-if="u.pro_expires_at">{{ fmtDate(u.pro_expires_at) }}</span>
                <span v-else-if="u.pro_plan === 'lifetime'" class="text-success">永久</span>
                <span v-else class="text-muted">—</span>
              </td>
              <td><span :class="['badge', u.is_active ? 'badge-success' : 'badge-danger']">{{ u.is_active ? '正常' : '封禁' }}</span></td>
              <td><button class="btn btn-ghost btn-sm" @click="openDetail(u.id)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg>
              </button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="pagination">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; loadUsers()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <span class="text-xs text-muted">{{ page }} / {{ totalPages }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= totalPages" @click="page++; loadUsers()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>
    </div>

    <!-- Detail Modal -->
    <div v-if="detailUser" class="modal-overlay" @click.self="detailUser = null">
      <div class="modal modal-lg">
        <div class="flex justify-between items-center" style="margin-bottom:20px">
          <h2>{{ detailUser.user.username }}</h2>
          <button class="btn-icon" @click="detailUser = null">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="detail-grid">
          <div class="detail-section">
            <h4 class="section-label">基本信息</h4>
            <div class="info-list">
              <div class="info-item"><span>邮箱</span><span>{{ detailUser.user.email }}</span></div>
              <div v-if="detailUser.user.google_email" class="info-item">
                <span>Google 账号</span>
                <span style="color: #4285F4;">{{ detailUser.user.google_email }}</span>
              </div>
              <div class="info-item">
                <span>密码</span>
                <span v-if="detailUser.user.password && detailUser.user.password !== '—'" class="password-cell">
                  <code v-if="showDetailPw" class="pw-text">{{ detailUser.user.password }}</code>
                  <span v-else class="pw-masked">••••••</span>
                  <button class="btn-icon-tiny" @click="showDetailPw = !showDetailPw">
                    <svg v-if="showDetailPw" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  </button>
                </span>
                <span v-else class="text-xs text-muted">待用户登录后采集</span>
              </div>
              <div class="info-item"><span>注册</span><span>{{ fmtDate(detailUser.user.created_at) }}</span></div>
              <div class="info-item"><span>最后登录</span><span>{{ fmtDate(detailUser.user.last_login_at) || '—' }}</span></div>
              <div class="info-item"><span>登录次数</span><span>{{ detailUser.user.login_count }}</span></div>
              <div class="info-item">
                <span>在线时长</span>
                <span class="online-duration">{{ fmtDuration(detailUser.user.total_online_seconds) }}</span>
              </div>
              <div class="info-item"><span>APP 创建</span><span>{{ detailUser.user.apps_created }}</span></div>
              <div class="info-item"><span>APK 构建</span><span>{{ detailUser.user.apks_built }}</span></div>
            </div>
          </div>
          <div class="detail-section">
            <h4 class="section-label">订阅管理</h4>
            <div class="form-group">
              <label class="form-label">套餐</label>
              <select v-model="editForm.pro_plan" class="select">
                <option value="free">Free</option>
                <option value="pro_monthly">Pro 月付 $3</option>
                <option value="pro_yearly">Pro 年付 $28.80</option>
                <option value="pro_lifetime">Pro 终身 $99</option>
                <option value="ultra_monthly">Ultra 月付 $9</option>
                <option value="ultra_yearly">Ultra 年付 $86.40</option>
                <option value="ultra_lifetime">Ultra 终身 $199</option>
                <option value="lifetime">终身(旧)</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">到期时间</label>
              <input v-model="editForm.pro_expires_at" type="datetime-local" class="input" />
            </div>
            <div class="flex gap-12">
              <div class="form-group" style="flex:1">
                <label class="form-label">最大设备数</label>
                <input v-model.number="editForm.max_devices" type="number" class="input" min="1" />
              </div>
              <div class="form-group" style="flex:1">
                <label class="form-label">项目上限</label>
                <input v-model.number="editForm.custom_project_limit" type="number" class="input" min="0" />
              </div>
            </div>
            <div class="flex gap-16 mt-8">
              <label class="toggle-label"><input type="checkbox" v-model="editForm.is_active" class="toggle" /> 账号正常</label>
            </div>
            <button class="btn btn-primary btn-sm mt-16" @click="saveUser" :disabled="saving" style="width:100%">保存修改</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, reactive, computed, watch } from 'vue'
import { adminApi } from '../api'
import api from '../api'

const showToast = inject('showToast')
const users = ref([]), loading = ref(false), search = ref('')
const planFilter = ref(''), statusFilter = ref('')
const page = ref(1), total = ref(0), totalPages = ref(1)
const detailUser = ref(null), editForm = ref({}), saving = ref(false)
const showDetailPw = ref(false)

let timer
function debouncedLoad() { clearTimeout(timer); timer = setTimeout(() => { page.value = 1; loadUsers() }, 400) }

async function loadUsers() {
  loading.value = true
  try {
    const res = await adminApi.listUsers({ page: page.value, page_size: 20, search: search.value || undefined, plan_filter: planFilter.value || undefined, status_filter: statusFilter.value || undefined })
    // Add _showPw reactive property to each user
    users.value = res.data.map(u => reactive({ ...u, _showPw: false }))
    total.value = res.total; totalPages.value = res.total_pages
  } catch {} finally { loading.value = false }
}

async function openDetail(id) {
  showDetailPw.value = false
  try {
    const res = await adminApi.getUser(id)
    detailUser.value = res.data
    editForm.value = {
      is_pro: res.data.user.is_pro, pro_plan: res.data.user.pro_plan || 'free',
      pro_expires_at: res.data.user.pro_expires_at?.slice(0, 16) || '',
      max_devices: res.data.user.max_devices, custom_project_limit: res.data.user.custom_project_limit || 0,
      is_active: res.data.user.is_active,
    }
  } catch { showToast('加载失败', 'error') }
}

async function saveUser() {
  saving.value = true
  try {
    const payload = {
      is_pro: editForm.value.is_pro,
      pro_plan: editForm.value.pro_plan,
      is_active: editForm.value.is_active,
      max_devices: editForm.value.max_devices,
      custom_project_limit: editForm.value.custom_project_limit || 0,
    }
    if (editForm.value.pro_expires_at) {
      payload.pro_expires_at = editForm.value.pro_expires_at
    }
    await adminApi.updateUser(detailUser.value.user.id, payload)
    showToast('已保存'); loadUsers(); openDetail(detailUser.value.user.id)
  } catch { showToast('保存失败', 'error') }
  finally { saving.value = false }
}

function togglePw(u) { u._showPw = !u._showPw }

function planName(p) { return { free:'Free', pro_monthly:'Pro 月', pro_yearly:'Pro 年', pro_lifetime:'Pro 终身', ultra_monthly:'Ultra 月', ultra_yearly:'Ultra 年', ultra_lifetime:'Ultra 终身', lifetime:'终身(旧)' }[p] || p }
function planColor(p) { if (p?.startsWith('ultra')) return 'badge-warning'; if (p?.startsWith('pro') || p === 'lifetime') return 'badge-info'; return 'badge-secondary' }
function pLimit(u) { if (u.custom_project_limit) return u.custom_project_limit; if (u.pro_plan?.startsWith('ultra')) return 50; if (u.pro_plan?.startsWith('pro') || u.pro_plan === 'lifetime') return 10; return 0 }


const membershipLabel = computed(() => {
  const p = editForm.value.pro_plan
  if (p?.startsWith('ultra')) return 'Ultra 会员'
  if (p?.startsWith('pro') || p === 'lifetime') return 'Pro 会员'
  return '会员'
})
// 选择非 free 套餐时自动勾选会员标识
watch(() => editForm.value.pro_plan, (plan) => {
  if (plan && plan !== 'free') editForm.value.is_pro = true
  else editForm.value.is_pro = false
})
function fmtDate(d) { return d ? new Date(d).toLocaleString('zh-CN', { month:'short', day:'numeric', hour:'2-digit', minute:'2-digit' }) : '' }

function fmtDuration(seconds) {
  if (!seconds || seconds <= 0) return '—'
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (d > 0) return `${d}天 ${h}小时`
  if (h > 0) return `${h}小时 ${m}分`
  return `${m}分钟`
}

onMounted(loadUsers)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.filter-row { display: flex; gap: 10px; flex-wrap: wrap; }
.search-wrap { position: relative; flex: 1; min-width: 200px; max-width: 320px; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; }
.search-input { padding-left: 36px; }

.user-row { transition: background var(--t-fast); }
.user-cell { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 32px; height: 32px; min-width: 32px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.75rem; font-weight: 600;
  background: linear-gradient(135deg, rgba(99,102,241,0.2), rgba(167,139,250,0.2));
  color: var(--accent-hover);
}
.user-name { font-weight: 500; font-size: 0.87rem; color: var(--text-primary); }

.password-cell { display: flex; align-items: center; gap: 4px; }
.pw-text { font-size: 0.8rem; padding: 2px 6px; background: var(--bg-input); border-radius: 4px; color: var(--text-primary); font-family: 'SF Mono', 'Fira Code', monospace; }
.pw-masked { font-size: 0.8rem; color: var(--text-muted); letter-spacing: 2px; }
.btn-icon-tiny {
  background: none; border: none; cursor: pointer; padding: 2px;
  color: var(--text-muted); opacity: 0.6; transition: opacity 0.2s;
  display: flex; align-items: center;
}
.btn-icon-tiny:hover { opacity: 1; }

.online-duration { font-weight: 500; color: var(--accent); }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.detail-section { background: var(--bg-input); border-radius: var(--r-md); padding: 18px; }
.section-label { font-size: 0.8rem; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 14px; }
.info-list { display: flex; flex-direction: column; gap: 0; }
.info-item { display: flex; justify-content: space-between; padding: 8px 0; font-size: 0.85rem; border-bottom: 1px solid var(--border-light); }
.info-item span:first-child { color: var(--text-muted); }
.info-item:last-child { border-bottom: none; }
.toggle-label { display: flex; align-items: center; gap: 6px; font-size: 0.85rem; color: var(--text-secondary); cursor: pointer; }
.toggle { accent-color: var(--accent); }
</style>
