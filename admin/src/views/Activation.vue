<template>
  <div>
    <h2 class="page-title">🔑 激活码管理</h2>

    <!-- Generate section -->
    <div class="card mb-20">
      <div class="card-header">
        <span class="card-title">生成激活码</span>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">会员层级</label>
          <select v-model="genForm.tier" class="select">
            <option value="pro">Pro</option>
            <option value="ultra">Ultra</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">计划类型</label>
          <select v-model="genForm.plan_type" class="select">
            <option :value="genForm.tier + '_monthly'">月度 (30天)</option>
            <option :value="genForm.tier + '_yearly'">年度 (365天)</option>
            <option :value="genForm.tier + '_lifetime'">终身</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">数量</label>
          <input v-model.number="genForm.count" type="number" class="input" min="1" max="500" />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">备注 (可选)</label>
        <input v-model="genForm.batch_note" class="input" placeholder="例如：Gumroad 2026-03" />
      </div>
      <div class="flex items-center gap-8">
        <button class="btn btn-primary" @click="generateCodes" :disabled="generating">
          {{ generating ? '生成中...' : '🔑 生成激活码' }}
        </button>
      </div>

      <!-- Generated codes -->
      <div v-if="generatedCodes.length" class="generated-codes mt-20">
        <div class="flex items-center justify-between mb-12">
          <span class="text-success">✅ 已生成 {{ generatedCodes.length }} 个激活码</span>
          <button class="btn btn-secondary btn-sm" @click="copyCodes">📋 复制全部</button>
        </div>
        <div class="codes-list">
          <code v-for="c in generatedCodes" :key="c">{{ c }}</code>
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-grid mb-20">
      <div class="stat-card purple">
        <div class="stat-label">总计</div>
        <div class="stat-value">{{ stats.total || 0 }}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-label">未使用</div>
        <div class="stat-value">{{ stats.unused || 0 }}</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-label">已使用</div>
        <div class="stat-value">{{ stats.used || 0 }}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-label">使用率</div>
        <div class="stat-value">{{ stats.usage_rate || 0 }}%</div>
      </div>
    </div>

    <!-- List -->
    <div class="card">
      <div class="card-header">
        <span class="card-title">激活码列表</span>
        <button class="btn btn-secondary btn-sm" @click="exportCodes">📥 导出 CSV</button>
      </div>

      <div class="toolbar">
        <select v-model="filter.status" class="select" style="max-width:140px" @change="page=1;loadCodes()">
          <option :value="null">全部状态</option>
          <option value="unused">未使用</option>
          <option value="used">已使用</option>
          <option value="disabled">已禁用</option>
        </select>
        <select v-model="filter.plan_type" class="select" style="max-width:140px" @change="page=1;loadCodes()">
          <option :value="null">全部类型</option>
          <option value="monthly">月度 (旧)</option>
          <option value="pro_monthly">Pro 月度</option>
          <option value="pro_yearly">Pro 年度</option>
          <option value="pro_lifetime">Pro 终身</option>
          <option value="ultra_monthly">Ultra 月度</option>
          <option value="ultra_yearly">Ultra 年度</option>
          <option value="ultra_lifetime">Ultra 终身</option>
          <option value="quarterly">季度 (旧)</option>
          <option value="yearly">年度 (旧)</option>
          <option value="lifetime">终身 (旧)</option>
        </select>
      </div>

      <div v-if="loading" class="spinner"></div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>激活码</th><th>类型</th><th>天数</th><th>状态</th><th>使用者</th><th>批次</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in codes" :key="c.id">
              <td><code style="font-size:12px">{{ c.code }}</code></td>
              <td>{{ c.plan_type }}</td>
              <td>{{ c.duration_days }}</td>
              <td>
                <span :class="['badge', statusBadge(c.status)]">{{ c.status }}</span>
              </td>
              <td>{{ c.used_by || '-' }}</td>
              <td class="text-muted" style="font-size:12px">{{ c.batch_id || '-' }}</td>
              <td>
                <button v-if="c.status === 'unused'" class="btn btn-danger btn-sm"
                  @click="disableCode(c.id)">禁用</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!codes.length" class="empty-state"><p>暂无激活码</p></div>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page <= 1" @click="page--; loadCodes()">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadCodes()">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, watch } from 'vue'
import { activationApi } from '../api'

const showToast = inject('showToast')
const genForm = ref({ tier: 'pro', plan_type: 'pro_monthly', count: 10, batch_note: '' })

// 切换会员层级时自动更新 plan_type
watch(() => genForm.value.tier, (newTier, oldTier) => {
  const current = genForm.value.plan_type
  if (current.endsWith('_lifetime')) {
    genForm.value.plan_type = newTier + '_lifetime'
    return
  }
  if (current === 'lifetime') return // 旧终身不需要切换
  // 将旧 tier 前缀替换为新 tier 前缀
  if (current.startsWith(oldTier + '_')) {
    genForm.value.plan_type = current.replace(oldTier + '_', newTier + '_')
  } else {
    genForm.value.plan_type = newTier + '_monthly'
  }
})
const generatedCodes = ref([])
const generating = ref(false)
const stats = ref({})
const codes = ref([])
const loading = ref(true)
const page = ref(1)
const totalPages = ref(1)
const filter = ref({ status: null, plan_type: null })

async function generateCodes() {
  generating.value = true
  try {
    const res = await activationApi.generate(genForm.value)
    generatedCodes.value = res.data.codes
    showToast(`已生成 ${res.data.count} 个激活码`)
    loadStats()
    loadCodes()
  } catch (e) { showToast(e?.detail || '生成失败', 'error') }
  finally { generating.value = false }
}

function copyCodes() {
  navigator.clipboard.writeText(generatedCodes.value.join('\n'))
  showToast('已复制到剪贴板')
}

async function loadStats() {
  try {
    const res = await activationApi.stats()
    stats.value = res.data
  } catch (e) { console.error(e) }
}

async function loadCodes() {
  loading.value = true
  try {
    const params = { page: page.value, page_size: 20 }
    if (filter.value.status) params.status = filter.value.status
    if (filter.value.plan_type) params.plan_type = filter.value.plan_type
    const res = await activationApi.list(params)
    codes.value = res.data
    totalPages.value = res.total_pages
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function disableCode(id) {
  if (!confirm('确定禁用此激活码？')) return
  try {
    await activationApi.disable(id)
    showToast('已禁用')
    loadCodes()
    loadStats()
  } catch (e) { showToast(e?.detail || '操作失败', 'error') }
}

async function exportCodes() {
  try {
    const res = await activationApi.exportCsv({ status: filter.value.status || 'unused' })
    const url = URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a')
    a.href = url; a.download = 'activation_codes.csv'; a.click()
    URL.revokeObjectURL(url)
  } catch (e) { showToast('导出失败', 'error') }
}

function statusBadge(s) {
  return { unused: 'badge-success', used: 'badge-info', disabled: 'badge-danger', expired: 'badge-warning' }[s] || 'badge-info'
}

onMounted(() => { loadStats(); loadCodes() })
</script>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 12px; }
.codes-list { display: flex; flex-wrap: wrap; gap: 8px; max-height: 200px; overflow-y: auto; }
.codes-list code {
  padding: 6px 12px; background: var(--bg-input); border: 1px solid var(--border);
  border-radius: 6px; font-size: 13px; color: var(--accent);
}
@media (max-width: 900px) { .stats-grid { grid-template-columns: repeat(2,1fr); } }
</style>
