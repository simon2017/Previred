import { useState } from 'react';
import type { FormEvent } from 'react';
import type { CampoError, PersonaRequest } from '../api/types';
import { esRutValido } from '../validation/rut';

interface Props {
  modo: 'crear' | 'editar';
  valoresIniciales?: PersonaRequest;
  erroresServidor: CampoError[];
  enviando: boolean;
  onSubmit: (data: PersonaRequest) => void;
  onCancelar: () => void;
}

const VACIO: PersonaRequest = {
  rut: '',
  nombre: '',
  apellido: '',
  fechaNacimiento: '',
  direccion: { calle: '', comuna: '', region: '' },
};

/** Valida en el cliente para dar feedback inmediato; el backend es la fuente de verdad. */
function validarLocal(data: PersonaRequest, modo: 'crear' | 'editar'): Record<string, string> {
  const errores: Record<string, string> = {};
  if (modo === 'crear' && !esRutValido(data.rut)) {
    errores.rut = 'El rut ingresado no es valido';
  }
  if (!data.nombre.trim()) errores.nombre = 'El nombre es obligatorio';
  if (!data.apellido.trim()) errores.apellido = 'El apellido es obligatorio';
  if (!data.fechaNacimiento) {
    errores.fechaNacimiento = 'La fecha de nacimiento es obligatoria';
  } else if (data.fechaNacimiento > new Date().toISOString().slice(0, 10)) {
    errores.fechaNacimiento = 'La fecha no puede ser futura';
  }
  if (!data.direccion.calle.trim()) errores['direccion.calle'] = 'La calle es obligatoria';
  if (!data.direccion.comuna.trim()) errores['direccion.comuna'] = 'La comuna es obligatoria';
  if (!data.direccion.region.trim()) errores['direccion.region'] = 'La region es obligatoria';
  return errores;
}

export default function PersonaForm({
  modo,
  valoresIniciales,
  erroresServidor,
  enviando,
  onSubmit,
  onCancelar,
}: Props) {
  const [data, setData] = useState<PersonaRequest>(valoresIniciales ?? VACIO);
  const [erroresLocales, setErroresLocales] = useState<Record<string, string>>({});

  function errorPara(campo: string): string | undefined {
    return (
      erroresLocales[campo] ?? erroresServidor.find((e) => e.campo === campo)?.mensaje
    );
  }

  function actualizarCampo(campo: keyof Omit<PersonaRequest, 'direccion'>, valor: string) {
    setData((prev) => ({ ...prev, [campo]: valor }));
  }

  function actualizarDireccion(campo: keyof PersonaRequest['direccion'], valor: string) {
    setData((prev) => ({ ...prev, direccion: { ...prev.direccion, [campo]: valor } }));
  }

  function manejarSubmit(e: FormEvent) {
    e.preventDefault();
    const errores = validarLocal(data, modo);
    setErroresLocales(errores);
    if (Object.keys(errores).length === 0) {
      onSubmit(data);
    }
  }

  return (
    <form className="formulario" onSubmit={manejarSubmit} noValidate>
      <div className="campo">
        <label htmlFor="rut">Rut</label>
        <input
          id="rut"
          value={data.rut}
          disabled={modo === 'editar'}
          placeholder="12345678-9"
          onChange={(e) => actualizarCampo('rut', e.target.value)}
        />
        {errorPara('rut') && <span className="campo-error">{errorPara('rut')}</span>}
      </div>

      <div className="campo">
        <label htmlFor="nombre">Nombre</label>
        <input id="nombre" value={data.nombre} onChange={(e) => actualizarCampo('nombre', e.target.value)} />
        {errorPara('nombre') && <span className="campo-error">{errorPara('nombre')}</span>}
      </div>

      <div className="campo">
        <label htmlFor="apellido">Apellido</label>
        <input
          id="apellido"
          value={data.apellido}
          onChange={(e) => actualizarCampo('apellido', e.target.value)}
        />
        {errorPara('apellido') && <span className="campo-error">{errorPara('apellido')}</span>}
      </div>

      <div className="campo">
        <label htmlFor="fechaNacimiento">Fecha de nacimiento</label>
        <input
          id="fechaNacimiento"
          type="date"
          value={data.fechaNacimiento}
          onChange={(e) => actualizarCampo('fechaNacimiento', e.target.value)}
        />
        {errorPara('fechaNacimiento') && (
          <span className="campo-error">{errorPara('fechaNacimiento')}</span>
        )}
      </div>

      <fieldset className="direccion">
        <legend>Direccion</legend>

        <div className="campo">
          <label htmlFor="calle">Calle</label>
          <input
            id="calle"
            value={data.direccion.calle}
            onChange={(e) => actualizarDireccion('calle', e.target.value)}
          />
          {errorPara('direccion.calle') && (
            <span className="campo-error">{errorPara('direccion.calle')}</span>
          )}
        </div>

        <div className="campo">
          <label htmlFor="comuna">Comuna</label>
          <input
            id="comuna"
            value={data.direccion.comuna}
            onChange={(e) => actualizarDireccion('comuna', e.target.value)}
          />
          {errorPara('direccion.comuna') && (
            <span className="campo-error">{errorPara('direccion.comuna')}</span>
          )}
        </div>

        <div className="campo">
          <label htmlFor="region">Region</label>
          <input
            id="region"
            value={data.direccion.region}
            onChange={(e) => actualizarDireccion('region', e.target.value)}
          />
          {errorPara('direccion.region') && (
            <span className="campo-error">{errorPara('direccion.region')}</span>
          )}
        </div>
      </fieldset>

      <div className="formulario-acciones">
        <button type="button" className="btn" onClick={onCancelar} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-primario" disabled={enviando}>
          {enviando ? 'Guardando...' : modo === 'crear' ? 'Crear' : 'Guardar cambios'}
        </button>
      </div>
    </form>
  );
}
