<script setup>
// 解决方案编辑：基本信息、图片、富文本详情、交付与服务说明、版本定价、增值服务，一次保存
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api.js'
import ImageUpload from '../components/ImageUpload.vue'
import GalleryUpload from '../components/GalleryUpload.vue'
import RichEditor from '../components/RichEditor.vue'

const route = useRoute()
const router = useRouter()
const isNew = computed(() => route.path.endsWith('/new'))
const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const categories = ref([])

const blankVersion = () => ({ name: '', price: null, originalPrice: null, billingType: 'ONE_TIME', description: '', status: 1 })
const blankService = () => ({ name: '', price: null, unit: '次', description: '', status: 1 })
const form = reactive({
  categoryId: null, slug: '', name: '', subtitle: '', summary: '', mainImage: '', gallery: [], content: '',
  deliveryMode: 'REMOTE_DEPLOY', afterSales: '', servicePeriod: '', recommended: 0, status: 0, sort: 0,
  versions: [blankVersion()], services: [],
})

const rules = {
  categoryId: [{ required: true, message: '请选择分类' }],
  slug: [
    { required: true, message: '请填写 URL 标识' },
    { pattern: /^[a-z0-9]+(-[a-z0-9]+)*$/, message: '只能是小写字母、数字和短横线，如 data-fusion' },
  ],
  name: [{ required: true, message: '请填写标题' }],
}

onMounted(async () => {
  categories.value = await api.categories()
  if (!isNew.value) {
    loading.value = true
    try {
      const d = await api.solution(route.params.id)
      Object.assign(form, {
        categoryId: d.categoryId, slug: d.slug, name: d.name, subtitle: d.subtitle || '', summary: d.summary || '',
        mainImage: d.mainImage || '', gallery: d.gallery || [], content: d.content || '', deliveryMode: d.deliveryMode,
        afterSales: d.afterSales || '', servicePeriod: d.servicePeriod || '', recommended: d.recommended, status: d.status, sort: d.sort,
        versions: d.versions.map((v) => ({ id: v.id, name: v.name, price: Number(v.price), originalPrice: v.originalPrice == null ? null : Number(v.originalPrice), billingType: v.billingType, description: v.description || '', status: v.status })),
        services: d.services.map((s) => ({ id: s.id, name: s.name, price: Number(s.price), unit: s.unit, description: s.description || '', status: s.status })),
      })
    } finally {
      loading.value = false
    }
  } else if (categories.value.length) {
    form.categoryId = categories.value[0].id
  }
})

function move(list, i, d) {
  ;[list[i], list[i + d]] = [list[i + d], list[i]]
}

function checkRows() {
  if (!form.versions.length) return '至少需要一个版本'
  for (const [i, v] of form.versions.entries()) {
    if (!v.name.trim()) return `第 ${i + 1} 个版本缺少名称`
    if (!(v.price > 0)) return `版本「${v.name}」的价格必须大于 0`
    if (v.originalPrice != null && v.originalPrice !== '' && v.originalPrice < v.price) return `版本「${v.name}」的原价不能低于售价`
  }
  for (const [i, s] of form.services.entries()) {
    if (!s.name.trim()) return `第 ${i + 1} 个增值服务缺少名称`
    if (!(s.price > 0)) return `增值服务「${s.name}」的价格必须大于 0`
    if (!s.unit.trim()) return `增值服务「${s.name}」缺少计价单位`
  }
  if (form.status === 1 && !form.versions.some((v) => v.status === 1)) return '上架状态下至少需要一个在售版本'
  return null
}

async function save() {
  await formRef.value.validate()
  const err = checkRows()
  if (err) return ElMessage.error(err)
  saving.value = true
  const body = {
    ...form,
    versions: form.versions.map((v, i) => ({ ...v, originalPrice: v.originalPrice === '' ? null : v.originalPrice, sort: i + 1 })),
    services: form.services.map((s, i) => ({ ...s, sort: i + 1 })),
  }
  try {
    const d = isNew.value ? await api.createSolution(body) : await api.updateSolution(route.params.id, body)
    ElMessage.success(form.status ? '已保存，前台即时生效' : '已保存（当前为下架状态，前台不展示）')
    if (isNew.value) router.replace(`/solutions/${d.id}`)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-loading="loading" class="page-card">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="form">
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="16">
        <el-col :md="12"><el-form-item label="标题" prop="name"><el-input v-model="form.name" maxlength="100" show-word-limit /></el-form-item></el-col>
        <el-col :md="12">
          <el-form-item label="URL 标识" prop="slug">
            <el-input v-model="form.slug" maxlength="64" placeholder="如 data-fusion"><template #prepend>/solutions/</template></el-input>
          </el-form-item>
        </el-col>
        <el-col :md="12">
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" style="width: 100%"><el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" /></el-select>
          </el-form-item>
        </el-col>
        <el-col :md="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" /><span class="muted ml">小的在前</span></el-form-item></el-col>
      </el-row>
      <el-form-item label="副标题"><el-input v-model="form.subtitle" maxlength="200" show-word-limit /></el-form-item>
      <el-form-item label="一句话简介"><el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
      <el-form-item label="主图"><ImageUpload v-model="form.mainImage" /></el-form-item>
      <el-form-item label="图集"><GalleryUpload v-model="form.gallery" /></el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status"><el-radio :value="1">上架</el-radio><el-radio :value="0">下架</el-radio></el-radio-group>
        <el-checkbox v-model="form.recommended" :true-value="1" :false-value="0" class="ml">首页推荐</el-checkbox>
      </el-form-item>

      <el-divider content-position="left">版本与定价</el-divider>
      <el-table :data="form.versions" border size="small" class="rows">
        <el-table-column label="版本名" min-width="120"><template #default="{ row }"><el-input v-model="row.name" maxlength="50" placeholder="如 标准版" /></template></el-table-column>
        <el-table-column label="售价（元）" width="150"><template #default="{ row }"><el-input-number v-model="row.price" :min="0.01" :precision="2" :step="100" controls-position="right" style="width: 130px" /></template></el-table-column>
        <el-table-column label="原价（选填）" width="150"><template #default="{ row }"><el-input-number v-model="row.originalPrice" :min="0.01" :precision="2" :step="100" controls-position="right" style="width: 130px" /></template></el-table-column>
        <el-table-column label="计费方式" width="140">
          <template #default="{ row }">
            <el-select v-model="row.billingType"><el-option label="一次性买断" value="ONE_TIME" /><el-option label="按年订阅" value="YEARLY" /></el-select>
          </template>
        </el-table-column>
        <el-table-column label="版本差异说明" min-width="200"><template #default="{ row }"><el-input v-model="row.description" maxlength="500" /></template></el-table-column>
        <el-table-column label="在售" width="70"><template #default="{ row }"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" /></template></el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ $index }">
            <el-button link :disabled="$index === 0" @click="move(form.versions, $index, -1)">上移</el-button>
            <el-button link type="danger" :disabled="form.versions.length === 1" @click="form.versions.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button class="add" icon="Plus" @click="form.versions.push(blankVersion())">添加版本</el-button>

      <el-divider content-position="left">增值服务（运维期、部署、培训等）</el-divider>
      <el-table :data="form.services" border size="small" class="rows" empty-text="暂无增值服务">
        <el-table-column label="服务名" min-width="140"><template #default="{ row }"><el-input v-model="row.name" maxlength="50" placeholder="如 延长运维服务" /></template></el-table-column>
        <el-table-column label="价格（元）" width="150"><template #default="{ row }"><el-input-number v-model="row.price" :min="0.01" :precision="2" :step="100" controls-position="right" style="width: 130px" /></template></el-table-column>
        <el-table-column label="计价单位" width="110"><template #default="{ row }"><el-input v-model="row.unit" maxlength="20" placeholder="年/次/人天" /></template></el-table-column>
        <el-table-column label="说明" min-width="200"><template #default="{ row }"><el-input v-model="row.description" maxlength="500" /></template></el-table-column>
        <el-table-column label="在售" width="70"><template #default="{ row }"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" /></template></el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ $index }">
            <el-button link :disabled="$index === 0" @click="move(form.services, $index, -1)">上移</el-button>
            <el-button link type="danger" @click="form.services.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button class="add" icon="Plus" @click="form.services.push(blankService())">添加增值服务</el-button>

      <el-divider content-position="left">交付与服务说明</el-divider>
      <el-form-item label="交付方式">
        <el-radio-group v-model="form.deliveryMode">
          <el-radio value="ONLINE_OPEN">在线开通</el-radio>
          <el-radio value="DOWNLOAD_LICENSE">下载授权</el-radio>
          <el-radio value="REMOTE_DEPLOY">远程部署</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="售后支持"><el-input v-model="form.afterSales" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
      <el-form-item label="服务期限"><el-input v-model="form.servicePeriod" maxlength="200" placeholder="如 含 1 年免费运维与版本升级" /></el-form-item>

      <el-divider content-position="left">详细介绍</el-divider>
      <el-form-item label-width="0"><RichEditor v-model="form.content" /></el-form-item>

      <div class="actions">
        <el-button @click="$router.push('/solutions')">返回列表</el-button>
        <el-button v-if="!isNew" tag="a" :href="`/solutions/${form.slug}`" target="_blank">前台预览</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.form { max-width: 1200px; }
.ml { margin-left: 12px; }
.rows { margin-bottom: 10px; }
.add { margin-bottom: 10px; }
.actions { position: sticky; bottom: 0; display: flex; justify-content: flex-end; gap: 10px; padding: 14px 0 4px; background: #fff; border-top: 1px solid #f0f0f0; }
</style>
