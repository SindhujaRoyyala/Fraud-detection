import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "SDDMS - Secure Document Management System",
  description: "Encrypted document and case management system",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
