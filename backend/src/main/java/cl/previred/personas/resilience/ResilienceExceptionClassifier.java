package cl.previred.personas.resilience;

import org.hibernate.exception.JDBCConnectionException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;

/**
 * Distingue una falla de <b>conectividad</b> hacia SQL Server (que activa la ruta de
 * contingencia) de un error de <b>negocio</b> (ej. una violacion de constraint), que
 * debe propagarse normalmente y nunca caer en el outbox.
 */
@Component
public class ResilienceExceptionClassifier {

    /** Recorre la cadena de causas buscando alguna que indique perdida de conectividad con la BD. */
    public boolean esFallaDeConectividad(Throwable ex) {
        Throwable actual = ex;
        while (actual != null) {
            if (actual instanceof DataAccessResourceFailureException
                    || actual instanceof CannotCreateTransactionException
                    || actual instanceof JDBCConnectionException
                    || actual instanceof SQLTransientConnectionException
                    || actual instanceof SQLNonTransientConnectionException) {
                return true;
            }
            if (actual instanceof SQLException sqlEx && esSqlStateDeConexion(sqlEx.getSQLState())) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private boolean esSqlStateDeConexion(String sqlState) {
        // Clase "08" del estandar SQL: "connection exception".
        return sqlState != null && sqlState.startsWith("08");
    }
}
