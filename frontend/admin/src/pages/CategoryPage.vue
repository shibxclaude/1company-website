<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api.js'

const rows = ref([])
const dialog = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ code: '', name: '', sort: 0, status: 1 })
const rules = {
  code: [{ required: true, message: '请填写编码' }, { pattern: /^[a-z0-9-]{1,50}$/, message: '小写字母、数字、短横线' }],
  name: [{ required: true, message: '请填写名称' }],
}

async function load() { rows.value = await api.categories() }
onMounted(load)

function open(row) {
  editingId.value = row?.id || null
  Object.assign(form, row ? { code: row.code, name: row.name, sort: row.sort, status: row.status } : { code: '', name: '', sort: rows.value.length + 1, status: 1 })
  dialog.value = true
}
async function save() {
  await formRef.value.validate()
  if (editingId.value) await api.updateCategory(editingId.value, form)
  else await api.createCategory(form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await api.deleteCategory(row.id)
  ElMessage.success('已删除')
  load()
}
</script>

<template>
  <div class="page-card">
    <div class="toolbar"><span class="spacer" /><el-button type="primary" icon="Plus" @click="open()">新增分类</el-button></div>
    <el-table :data="rows" border>
      <el-table-column prop="name" label="分类名" />
      <el-table-column prop="code" label="编码（前台筛选用）" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialog" :title="editingId ? '编辑分类' : '新增分类'" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" maxlength="50" /></el-form-item>
        <el-form-item label="编码" prop="code"><el-input v-model="form.code" maxlength="50" placeholder="如 police" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
