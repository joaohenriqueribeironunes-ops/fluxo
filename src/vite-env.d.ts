/// <reference types="vite/client" />

declare module '@/lib/utils' {
  export function cn(...inputs: ClassValue[]): string
}

declare module '@/components/ui/button' {
  export type ButtonVariant = 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
  export type ButtonSize = 'default' | 'sm' | 'lg' | 'xl' | 'icon'
  export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant
    size?: ButtonSize
    asChild?: boolean
    loading?: boolean
  }
  export const Button: React.ForwardRefExoticComponent<ButtonProps & React.RefAttributes<HTMLButtonElement>>
  export const buttonVariants: any
}

declare module '@/components/ui/input' {
  export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {}
  export const Input: React.ForwardRefExoticComponent<InputProps & React.RefAttributes<HTMLInputElement>>
}

declare module '@/components/ui/label' {
  export interface LabelProps extends React.LabelHTMLAttributes<HTMLLabelElement> {}
  export const Label: React.ForwardRefExoticComponent<LabelProps & React.RefAttributes<HTMLLabelElement>>
}

declare module '@/components/ui/card' {
  export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {}
  export const Card: React.ForwardRefExoticComponent<CardProps & React.RefAttributes<HTMLDivElement>>
  export const CardHeader: React.ForwardRefExoticComponent<CardProps & React.RefAttributes<HTMLDivElement>>
  export const CardTitle: React.ForwardRefExoticComponent<React.HTMLAttributes<HTMLHeadingElement> & React.RefAttributes<HTMLHeadingElement>>
  export const CardDescription: React.ForwardRefExoticComponent<React.HTMLAttributes<HTMLParagraphElement> & React.RefAttributes<HTMLParagraphElement>>
  export const CardContent: React.ForwardRefExoticComponent<CardProps & React.RefAttributes<HTMLDivElement>>
  export const CardFooter: React.ForwardRefExoticComponent<CardProps & React.RefAttributes<HTMLDivElement>>
}

declare module '@/components/ui/separator' {
  export interface SeparatorProps extends React.HTMLAttributes<HTMLDivElement> {
    orientation?: 'horizontal' | 'vertical'
    decorative?: boolean
  }
  export const Separator: React.ForwardRefExoticComponent<SeparatorProps & React.RefAttributes<HTMLDivElement>>
}

declare module '@/components/ui/auth-layout' {
  export interface AuthLayoutProps {
    icon: React.ComponentType<{ className?: string }>
    title: string
    subtitle?: string
    children: React.ReactNode
    footer?: React.ReactNode
  }
  export function AuthLayout(props: AuthLayoutProps): React.ReactElement
}