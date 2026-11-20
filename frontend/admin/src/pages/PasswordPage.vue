<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, auth } from '../api.js'

const formRef = ref()
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const rules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/, message: '8-32 位，需同时包含字母和数字' },
  ],
  confirm: [{ validator: (_, v, cb) => (v === form.newPassword ? cb() : cb(new Error('两次输入不一致'))) }],
}
async function save() {
  await formRef.value.validate()
  // 改密码后之前签发的登录凭证全部失效（其他设备需重新登录），当前页面换用新凭证
  const r = await api.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
  auth.set(r.token)
  ElMessage.success('密码已修改，其他设备上的登录已失效')
  Object.assign(form, { oldPassword: '', newPassword: '', confirm: '' })
}
</script>

<template>
  <div class="page-card" style="max-width: 480px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password /></el-form-item>
      <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password /></el-form-item>
      <el-form-item label="确认新密码" prop="confirm"><el-input v-model="form.confirm" type="password" show-password /></el-form-item>
      <el-form-item><el-button type="primary" @click="save">保存</el-button></el-form-item>
    </el-form>
  </div>
</template>
