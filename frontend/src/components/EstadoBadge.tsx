import type { EstadoSincronizacion } from '../api/types';

interface Props {
  estado: EstadoSincronizacion;
  mensaje: string | null;
}

const ETIQUETAS: Record<EstadoSincronizacion, string> = {
  SINCRONIZADO: 'Sincronizado',
  PENDIENTE_SINCRONIZACION: 'Pendiente de sincronizacion',
};

/** Distintivo visual del estado de sincronizacion de un registro con la BD. */
export default function EstadoBadge({ estado, mensaje }: Props) {
  const clase = estado === 'SINCRONIZADO' ? 'badge badge-ok' : 'badge badge-pendiente';
  return (
    <span className={clase} title={mensaje ?? undefined}>
      {ETIQUETAS[estado]}
    </span>
  );
}
