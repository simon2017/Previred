import { BrowserRouter, Route, Routes } from 'react-router-dom';
import PersonaFormPage from './pages/PersonaFormPage';
import {PersonaListPage} from './pages/PersonaListPage';

export default function App() {
  return (
    <BrowserRouter>
      <header className="app-header">
        <div className="app-header-contenido">Previred - Personas</div>
      </header>
      <main className="app-main">
        <Routes>
          <Route path="/" element={<PersonaListPage />} />
          <Route path="/personas/nueva" element={<PersonaFormPage />} />
          <Route path="/personas/:rut/editar" element={<PersonaFormPage />} />
        </Routes>
      </main>
    </BrowserRouter>
  );
}
