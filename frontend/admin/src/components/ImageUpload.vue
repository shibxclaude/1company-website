<script setup>
// 单图上传（主图、新闻封面）
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api.js'

const model = defineModel({ type: String, default: '' })
const uploading = ref(false)

async function pick(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (file.size > 5 * 1024 * 1024) return ElMessage.error('图片不能超过 5MB')
  uploading.value = true
  try {
    model.value = (await api.upload(file)).url
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="img-upload">
    <div v-if="model" class="preview">
      <img :src="model" alt="" />
      <el-button size="small" type="danger" text @click="model = ''">移除</el-button>
    </div>
    <label class="btn" :class="{ disabled: uploading }">
      <input type="file" accept="image/png,image/jpeg,image/gif,image/webp" hidden :disabled="uploading" @change="pick" />
      <el-button tag="span" :loading="uploading">{{ model ? '更换图片' : '上传图片' }}</el-button>
    </label>
    <el-input v-model="model" placeholder="或填写图片地址，如 /images/xxx.png" class="url" clearable />
  </div>
</template>

<style scoped>
.img-upload { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; width: 100%; }
.preview { display: flex; flex-direction: column; align-items: center; }
.preview img { width: 160px; height: 100px; object-fit: cover; border: 1px solid #eee; border-radius: 4px; }
.url { flex: 1; min-width: 220px; }
</style>
