interface Props {
  mensaje: string | null;
}

/** Aviso visual uniforme para errores de la API (mensaje general de un ErrorResponse). */
export default function ErrorBanner({ mensaje }: Props) {
  if (!mensaje) return null;
  return (
    <div className="error-banner" role="alert">
      {mensaje}
    </div>
  );
}
