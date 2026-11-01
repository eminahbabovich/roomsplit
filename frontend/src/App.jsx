import { Route, Routes } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth.jsx'
import { Layout } from './components/Layout.jsx'
import { GroupPage } from './pages/GroupPage.jsx'
import { GroupsPage } from './pages/GroupsPage.jsx'
import { Landing } from './pages/Landing.jsx'
import { Login, Register } from './pages/AuthPages.jsx'
import { NotFound } from './pages/NotFound.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Landing />} />
        <Route path="prijava" element={<Login />} />
        <Route path="registracija" element={<Register />} />

        {/* sve ispod zahtijeva prijavu */}
        <Route element={<RequireAuth />}>
          <Route path="grupe" element={<GroupsPage />} />
          <Route path="grupe/:id" element={<GroupPage />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
