<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api.js'
import ImageUpload from '../components/ImageUpload.vue'
import RichEditor from '../components/RichEditor.vue'

const route = useRoute()
const router = useRouter()
const isNew = computed(() => route.path.endsWith('/new'))
const formRef = ref()
const saving = ref(false)
const form = reactive({ title: '', summary: '', cover: '', content: '', publishedAt: '', top: 0, status: 1 })
const rules = { title: [{ required: true, message: '请填写标题' }] }

onMounted(async () => {
  if (!isNew.value) {
    const d = await api.news(route.params.id)
    Object.assign(form, { title: d.title, summary: d.summary || '', cover: d.cover || '', content: d.content || '', publishedAt: d.publishedAt, top: d.top, status: d.status })
  }
})

async function save() {
  await formRef.value.validate()
  if (!form.content || form.content === '<p><br></p>') return ElMessage.error('请填写正文')
  saving.value = true
  try {
    const body = { ...form, publishedAt: form.publishedAt || null }
    const d = isNew.value ? await api.createNews(body) : await api.updateNews(route.params.id, body)
    ElMessage.success('已保存')
    if (isNew.value) router.replace(`/news/${d.id}`)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-card">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 1100px">
      <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
      <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
      <el-form-item label="封面"><ImageUpload v-model="form.cover" /></el-form-item>
      <el-form-item label="发布时间">
        <el-date-picker v-model="form.publishedAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空为当前时间" />
        <span class="muted" style="margin-left: 8px">设置为将来的时间可定时发布</span>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status"><el-radio :value="1">发布</el-radio><el-radio :value="0">草稿</el-radio></el-radio-group>
        <el-checkbox v-model="form.top" :true-value="1" :false-value="0" style="margin-left: 12px">置顶</el-checkbox>
      </el-form-item>
      <el-form-item label="正文" required><RichEditor v-model="form.content" /></el-form-item>
      <el-form-item>
        <el-button @click="$router.push('/news')">返回列表</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
