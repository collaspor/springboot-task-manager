import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { listTasks, createTask, updateTask, deleteTask } from './api.js'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch })

test('列表请求传递筛选状态并解析任务', async () => {
  let requestedUrl
  globalThis.fetch = async (url) => {
    requestedUrl = url
    return { ok: true, status: 200, json: async () => [{ id: 1, title: '阅读文档' }] }
  }
  assert.deepEqual(await listTasks('active'), [{ id: 1, title: '阅读文档' }])
  assert.equal(requestedUrl, '/api/tasks?status=active')
})

test('新增和更新任务发送 JSON 数据', async () => {
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return { ok: true, status: 200, json: async () => ({ id: 7 }) }
  }
  await createTask({ title: '读书', description: '' })
  await updateTask(7, { title: '读书', description: '', completed: true })
  assert.equal(calls[0].options.method, 'POST')
  assert.equal(calls[0].options.headers['Content-Type'], 'application/json')
  assert.equal(JSON.parse(calls[0].options.body).title, '读书')
  assert.equal(calls[1].url, '/api/tasks/7')
  assert.equal(calls[1].options.method, 'PUT')
  assert.equal(JSON.parse(calls[1].options.body).completed, true)
})

test('删除任务接受 204 空响应', async () => {
  globalThis.fetch = async () => ({ ok: true, status: 204 })
  assert.equal(await deleteTask(7), undefined)
})

test('接口错误保留服务端消息', async () => {
  globalThis.fetch = async () => ({ ok: false, status: 404, json: async () => ({ message: '任务不存在' }) })
  await assert.rejects(() => deleteTask(7), /任务不存在/)
})
