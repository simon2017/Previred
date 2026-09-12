interface Props {
  titulo: string;
  mensaje: string;
  onConfirmar: () => void;
  onCancelar: () => void;
}

/** Modal simple de confirmacion, usado antes de eliminar una persona. */
export default function ConfirmDialog({ titulo, mensaje, onConfirmar, onCancelar }: Props) {
  return (
    <div className="dialog-overlay" onClick={onCancelar}>
      <div className="dialog" onClick={(e) => e.stopPropagation()}>
        <h3>{titulo}</h3>
        <p>{mensaje}</p>
        <div className="dialog-acciones">
          <button type="button" className="btn" onClick={onCancelar}>
            Cancelar
          </button>
          <button type="button" className="btn btn-peligro" onClick={onConfirmar}>
            Eliminar
          </button>
        </div>
      </div>
    </div>
  );
}
