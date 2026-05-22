<template>
  <div class="login-page">
    <div class="login-bg-pattern" aria-hidden="true" />
    <el-card class="login-card" shadow="never">
      <div class="login-brand">
        <span class="brand-mark">GR</span>
        <div>
          <h1>酒店客房管理系统</h1>
          <p>Guest Room Management System</p>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" class="submit-btn" size="large" :loading="loading" @click="onSubmit">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { resolveDefaultHomePath } from '@/utils/homeRoute'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    const fallback = resolveDefaultHomePath(auth.permissions)
    const redirect = (route.query.redirect as string) || fallback
    router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  background: linear-gradient(145deg, #0f172a 0%, #1e3a8a 45%, #1e40af 100%);
  overflow: hidden;
}

.login-bg-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 30%, rgba(59, 130, 246, 0.25) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(30, 64, 175, 0.2) 0%, transparent 45%);
  pointer-events: none;
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 8px 4px 4px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 25px 50px rgba(15, 23, 42, 0.25);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--grms-border);
}

.brand-mark {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #1e40af, #3b82f6);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
}

.login-brand h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--grms-text);
  letter-spacing: -0.02em;
}

.login-brand p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--grms-text-muted);
}

.login-form {
  margin-top: 4px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  font-weight: 600;
  letter-spacing: 0.04em;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.submit-btn:not(:disabled):hover {
  box-shadow: 0 8px 20px rgba(30, 64, 175, 0.35);
}

@media (prefers-reduced-motion: reduce) {
  .submit-btn:not(:disabled):hover {
    box-shadow: none;
  }
}

@media (max-width: 480px) {
  .login-card {
    padding: 4px;
  }
  .login-brand h1 {
    font-size: 18px;
  }
}
</style>
