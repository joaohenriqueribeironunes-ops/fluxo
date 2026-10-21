import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '@/components/ui/auth-layout'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Mail, LoaderCircle, ArrowLeft } from 'lucide-react'

export function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)

    // Simulate sending reset email
    await new Promise((resolve) => setTimeout(resolve, 1500))

    setLoading(false)
    setSent(true)
  }

  return (
    <AuthLayout
      icon={Mail}
      title={sent ? 'Check your email' : 'Reset password'}
      subtitle={sent
        ? "If an account exists with that email, you'll receive a password reset link shortly."
        : "We'll send you a link to reset it"}
      footer={
        <Link to="/login" className="text-primary font-medium hover:underline flex items-center justify-center gap-1">
          <ArrowLeft className="w-3 h-3" aria-hidden="true" />
          Back to log in
        </Link>
      }
    >
      {sent ? (
        <p className="text-sm text-foreground text-center">
          If an account exists with that email, you&apos;ll receive a password reset link shortly.
        </p>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="email">Email address</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" aria-hidden="true" />
              <Input
                id="email"
                type="email"
                autoComplete="email"
                autoFocus
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="pl-10 h-12"
                required
                disabled={loading}
              />
            </div>
          </div>

          <Button type="submit" className="w-full h-12 font-medium" disabled={loading || !email}>
            {loading ? (
              <>
                <LoaderCircle className="w-4 h-4 mr-2 animate-spin" />
                Sending...
              </>
            ) : (
              'Send reset link'
            )}
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}