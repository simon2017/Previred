import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, eliminarPersona, listarPersonas } from '../api/personasApi';
import type { PersonaResponse } from '../api/types';
import ConfirmDialog from '../components/ConfirmDialog';
import ErrorBanner from '../components/ErrorBanner';
import EstadoBadge from '../components/EstadoBadge';

export function PersonaListPage() {
  const [personas, setPersonas] = useState<PersonaResponse[]>([]);
  const [cargando, setCargando] = useState(true);
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [rutAEliminar, setRutAEliminar] = useState<string | null>(null);

  async function cargar() {
    setCargando(true);
    setErrorGeneral(null);
    try {
      setPersonas(await listarPersonas());
    } catch (err) {
      setErrorGeneral(err instanceof ApiError ? err.response.message : 'No fue posible cargar las personas');
    } finally {
      setCargando(false);
    }
  }

  useEffect(() => {
    cargar();
  }, []);

  async function confirmarEliminar() {
    if (!rutAEliminar) return;
    try {
      await eliminarPersona(rutAEliminar);
      setRutAEliminar(null);
      await cargar();
    } catch (err) {
      setRutAEliminar(null);
      setErrorGeneral(err instanceof ApiError ? err.response.message : 'No fue posible eliminar el registro');
    }
  }

  return (
      <div className="pagina">
        <div className="pagina-header">
          <h1>Personas</h1>
          <Link to="/personas/nueva" className="btn btn-primario">
            Crear persona
          </Link>
        </div>

        <ErrorBanner mensaje={errorGeneral}/>

        {cargando ? (
            <p>Cargando...</p>
        ) : personas.length === 0 ? (
            <p>No hay personas registradas.</p>
        ) : (
            <table className="tabla">
              <thead>
              <tr>
                <th>Rut</th>
                <th>Nombre</th>
                <th>Apellido</th>
                <th>Edad</th>
                <th>Direccion</th>
                <th>Estado</th>
                <th></th>
              </tr>
              </thead>
              <tbody>
              {personas.map((p) => (
                  <tr key={p.rut}>
                    <td>{p.rut}</td>
                    <td>{p.nombre}</td>
                    <td>{p.apellido}</td>
                    <td>{p.edad}</td>
                    <td>
                      {p.direccion.calle}, {p.direccion.comuna}, {p.direccion.region}
                    </td>
                    <td>
                      <EstadoBadge estado={p.estadoSincronizacion} mensaje={p.mensaje}/>
                    </td>
                    <td className="tabla-acciones">
                      <Link to={`/personas/${encodeURIComponent(p.rut)}/editar`} className="btn">
                        Editar
                      </Link>
                      <button type="button" className="btn btn-peligro" onClick={() => setRutAEliminar(p.rut)}>
                        Eliminar
                      </button>
                    </td>
                  </tr>
              ))}
              </tbody>
            </table>
        )}

        {rutAEliminar && (
            <ConfirmDialog
                titulo="Eliminar persona"
                mensaje={`¿Eliminar el registro con rut ${rutAEliminar}? Esta acción no se puede deshacer.`}
                onConfirmar={confirmarEliminar}
                onCancelar={() => setRutAEliminar(null)}
            />
        )}
      </div>
  );
}
