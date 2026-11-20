<script setup>
// 多图上传（图集），可调整顺序、删除
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api.js'

const model = defineModel({ type: Array, default: () => [] })
const props = defineProps({ max: { type: Number, default: 12 } })
const uploading = ref(false)

async function pick(e) {
  const files = [...(e.target.files || [])]
  e.target.value = ''
  if (!files.length) return
  if (model.value.length + files.length > props.max) return ElMessage.error(`最多 ${props.max} 张`)
  uploading.value = true
  try {
    const next = [...model.value]
    for (const f of files) {
      if (f.size > 5 * 1024 * 1024) {
        ElMessage.error(`${f.name} 超过 5MB，已跳过`)
        continue
      }
      next.push((await api.upload(f)).url)
    }
    model.value = next
  } finally {
    uploading.value = false
  }
}
function move(i, d) {
  const next = [...model.value]
  ;[next[i], next[i + d]] = [next[i + d], next[i]]
  model.value = next
}
function remove(i) {
  model.value = model.value.filter((_, j) => j !== i)
}
</script>

<template>
  <div class="gallery">
    <div v-for="(src, i) in model" :key="src + i" class="item">
      <img :src="src" alt="" />
      <div class="ops">
        <el-button size="small" text :disabled="i === 0" @click="move(i, -1)">←</el-button>
        <el-button size="small" text type="danger" @click="remove(i)">删除</el-button>
        <el-button size="small" text :disabled="i === model.length - 1" @click="move(i, 1)">→</el-button>
      </div>
    </div>
    <label v-if="model.length < max" class="add">
      <input type="file" accept="image/png,image/jpeg,image/gif,image/webp" multiple hidden :disabled="uploading" @change="pick" />
      <el-icon v-if="!uploading" :size="24"><Plus /></el-icon>
      <span>{{ uploading ? '上传中…' : '添加图片' }}</span>
    </label>
  </div>
</template>

<style scoped>
.gallery { display: flex; flex-wrap: wrap; gap: 10px; }
.item { width: 150px; border: 1px solid #eee; border-radius: 4px; overflow: hidden; }
.item img { display: block; width: 150px; height: 96px; object-fit: cover; }
.ops { display: flex; justify-content: space-between; }
.add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 150px;
  height: 126px;
  font-size: 13px;
  color: #909399;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
}
.add:hover { color: #409eff; border-color: #409eff; }
</style>
