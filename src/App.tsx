import { Routes, Route, Navigate } from 'react-router-dom'
import { AppShell } from '@/components/layout'
import { Login } from '@/pages/Login'
import { Register } from '@/pages/Register'
import { ForgotPassword } from '@/pages/ForgotPassword'
import { ResetPassword } from '@/pages/ResetPassword'
import { Focus } from '@/pages/Focus'
import { Game } from '@/pages/Game'
import { Frisson } from '@/pages/Frisson'
import { Ideas } from '@/pages/Ideas'
import { Settings } from '@/pages/Settings'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route element={<AppShell />}>
        <Route path="/" element={<Navigate to="/focus" replace />} />
        <Route path="/focus" element={<Focus />} />
        <Route path="/game" element={<Game />} />
        <Route path="/frisson" element={<Frisson />} />
        <Route path="/ideas" element={<Ideas />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="*" element={<Navigate to="/focus" replace />} />
      </Route>
    </Routes>
  )
}
