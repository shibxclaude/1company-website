<script setup>
// 富文本编辑器（wangEditor 5）。图片上传走后台文件接口；内容保存时后端会做白名单清洗。
import '@wangeditor/editor/dist/css/style.css'
import { shallowRef, onBeforeUnmount } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { ElMessage } from 'element-plus'
import { api } from '../api.js'

const model = defineModel({ type: String, default: '' })
defineProps({ height: { type: Number, default: 420 } })
const editorRef = shallowRef()

const toolbarConfig = { excludeKeys: ['group-video', 'insertVideo', 'uploadVideo', 'fullScreen'] }
const editorConfig = {
  placeholder: '请输入内容…',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        if (file.size > 5 * 1024 * 1024) return ElMessage.error('图片不能超过 5MB')
        const r = await api.upload(file)
        insertFn(r.url, file.name, r.url)
      },
    },
  },
}

onBeforeUnmount(() => editorRef.value?.destroy())
</script>

<template>
  <div class="rich-editor">
    <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" class="tb" />
    <Editor v-model="model" :default-config="editorConfig" mode="default" :style="{ height: `${height}px` }" @on-created="(e) => (editorRef = e)" />
  </div>
</template>

<style scoped>
.rich-editor { width: 100%; border: 1px solid #dcdfe6; border-radius: 4px; overflow: hidden; }
.tb { border-bottom: 1px solid #dcdfe6; }
</style>
