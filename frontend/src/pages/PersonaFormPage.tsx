import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError, actualizarPersona, crearPersona, obtenerPersona } from '../api/personasApi';
import type { CampoError, PersonaRequest } from '../api/types';
import ErrorBanner from '../components/ErrorBanner';
import PersonaForm from '../components/PersonaForm';

export default function PersonaFormPage() {
  const { rut } = useParams<{ rut?: string }>();
  const modo = rut ? 'editar' : 'crear';
  const navigate = useNavigate();

  const [valoresIniciales, setValoresIniciales] = useState<PersonaRequest | undefined>(undefined);
  const [cargando, setCargando] = useState(modo === 'editar');
  const [enviando, setEnviando] = useState(false);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [erroresServidor, setErroresServidor] = useState<CampoError[]>([]);

  useEffect(() => {
    if (modo !== 'editar' || !rut) return;
    obtenerPersona(rut)
      .then((persona) =>
        setValoresIniciales({
          rut: persona.rut,
          nombre: persona.nombre,
          apellido: persona.apellido,
          fechaNacimiento: persona.fechaNacimiento,
          direccion: persona.direccion,
        }),
      )
      .catch((err) =>
        setErrorGeneral(err instanceof ApiError ? err.response.message : 'No fue posible cargar el registro'),
      )
      .finally(() => setCargando(false));
  }, [modo, rut]);

  async function manejarSubmit(data: PersonaRequest) {
    setEnviando(true);
    setErrorGeneral(null);
    setErroresServidor([]);
    try {
      if (modo === 'crear') {
        await crearPersona(data);
      } else if (rut) {
        await actualizarPersona(rut, data);
      }
      navigate('/');
    } catch (err) {
      if (err instanceof ApiError) {
        setErroresServidor(err.response.fieldErrors);
        setErrorGeneral(err.response.message);
      } else {
        setErrorGeneral('No fue posible guardar el registro');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="pagina">
      <div className="pagina-header">
        <h1>{modo === 'crear' ? 'Crear persona' : 'Editar persona'}</h1>
      </div>

      <ErrorBanner mensaje={errorGeneral} />

      {cargando ? (
        <p>Cargando...</p>
      ) : modo === 'editar' && !valoresIniciales ? null : (
        <PersonaForm
          modo={modo}
          valoresIniciales={valoresIniciales}
          erroresServidor={erroresServidor}
          enviando={enviando}
          onSubmit={manejarSubmit}
          onCancelar={() => navigate('/')}
        />
      )}
    </div>
  );
}
