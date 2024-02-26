/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package OrionSFI.core.estandarAmortizaciones;

import OrionSFI.core.commons.JDBCConnectionPool;
import OrionSFI.core.commons.SQLProperties;
import OrionSFI.core.estandarTransacciones.EstandarTransacciones;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 *
 * @author Isaac Tenorio
 */
public class EstandarAmortizaciones {

    private ResultSet resultado = null;
    private StringBuffer query = null;
    
    public synchronized void recalculoIntereses(short numeroInstitucion,
            long numeroCuenta, String fechaAplicacion, String fechaProceso,
            String idUsuario, String macAddress, JDBCConnectionPool bd)
            throws Exception{
        
        SQLProperties sqlProperties = new SQLProperties();
        SimpleDateFormat formatoSoloFechaOrion = new SimpleDateFormat(sqlProperties.getFormatoSoloFechaOrion());
        List<String> listAmortizaciones = new ArrayList<String>();
        String fechaInicial = null;
        String fechaFinal = null;
        short numeroTabla = 0;
        int numeroAmortizacion = 0;
        String renglonDatos = null;
        StringTokenizer st = null;
        String separador = sqlProperties.getSeparador();
        EstandarTransacciones estandarTransacciones = new EstandarTransacciones();
        Map<String, String> mapInteresOrdinario = new HashMap<String, String>();
        double montoInteres = 0;
        double impuestoInteres = 0;
        double interesPagado = 0;
        double impuestoPagado = 0;
        // fecha actual
        Date fecha = new Date();
        String fechaActual = formatoSoloFechaOrion.format(fecha);
                
        byte isConexionAbierta = 0;
        if (bd != null) {
            isConexionAbierta = 1;
        }
        
        try {
            if (isConexionAbierta == 0) 
                bd = new JDBCConnectionPool();
            
            query = new StringBuffer("SELECT "); 
            query.append("FECHA_INICIAL, "); 
            query.append("FECHA_FINAL, "); 
            query.append("NUMERO_TABLA, "); 
            query.append("NUMERO_AMORTIZACION "); 
            query.append("FROM CRE_TABLA_AMORTIZACIONES "); 
            query.append("WHERE NUMERO_INSTITUCION= ").append(numeroInstitucion); 
            query.append(" AND NUMERO_CUENTA= ").append(numeroCuenta); 
            query.append(" AND FECHA_PAGO > '").append(sqlProperties.getFormatoFechaServidor(fechaAplicacion, sqlProperties.getFormatoFecha())).append("'");
            query.append(" AND STATUS=1");
            System.out.println(query);
            resultado = bd.executeQuery(query.toString());
            listAmortizaciones = sqlProperties.getColumnValueFormatoFecha(resultado, listAmortizaciones, sqlProperties.getFormatoSoloFechaOrion());
            
            for (int i = 0; i < listAmortizaciones.size(); i++) {
                renglonDatos = listAmortizaciones.get(i).toString();
                st = new StringTokenizer(renglonDatos, separador);
                fechaInicial = st.nextToken();
                fechaFinal = st.nextToken();
                numeroTabla = Short.parseShort(st.nextToken());
                numeroAmortizacion = Integer.parseInt(st.nextToken());
                
                mapInteresOrdinario = estandarTransacciones.getCalculoInteresOrdinarioCredito(
                        numeroInstitucion, numeroCuenta,
                        formatoSoloFechaOrion.parse(fechaInicial).before(formatoSoloFechaOrion.parse(fechaProceso))?fechaProceso:fechaInicial, 
                        fechaFinal, bd);

                if(formatoSoloFechaOrion.parse(fechaInicial).before(formatoSoloFechaOrion.parse(fechaProceso))){
                    query.setLength(0);
                    query.append("SELECT ");
                    query.append("SUM(INTERES_CALCULADO) AS SUM_INTERES_CALCULADO, ");
                    query.append("SUM(IMPUESTO_CALCULADO) AS SUM_IMPUESTO_CALCULADO ");
                    query.append("FROM CRE_INTERESES_DETALLE ");
                    query.append("WHERE NUMERO_INSTITUCION= ").append(numeroInstitucion);
                    query.append(" AND NUMERO_CUENTA= ").append(numeroCuenta);
                    query.append(" AND FECHA_INICIAL >= '").append(sqlProperties.getFormatoFechaServidor(fechaInicial, sqlProperties.getFormatoSoloFecha())).append("'");
                    query.append(" AND STATUS=1");
                    System.out.println(query);
                    resultado = bd.executeQuery(query.toString());
                    if(resultado.next()){
                        if(resultado.getString("SUM_INTERES_CALCULADO") != null &&
                                resultado.getString("SUM_IMPUESTO_CALCULADO") != null &&
                                resultado.getDouble("SUM_INTERES_CALCULADO") + resultado.getDouble("SUM_IMPUESTO_CALCULADO") > 0){
                            montoInteres = Double.parseDouble(mapInteresOrdinario.get("INTERES")) + resultado.getDouble("SUM_INTERES_CALCULADO");
                            impuestoInteres = Double.parseDouble(mapInteresOrdinario.get("IMPUESTO_INTERES")) + resultado.getDouble("SUM_IMPUESTO_CALCULADO");
                        }
                    }

                    query.setLength(0);
                    query.append("SELECT ");
                    query.append("SUM(MONTO_TRANSACCION) AS INTERES_PAGADO ");
                    query.append("FROM CRE_HISTORICO_MOVTOS ");
                    query.append("WHERE NUMERO_INSTITUCION= ").append(numeroInstitucion);
                    query.append(" AND NUMERO_CUENTA= ").append(numeroCuenta);
                    query.append(" AND NUMERO_TABLA= ").append(numeroTabla);
                    query.append(" AND NUMERO_AMORTIZACION= ").append(numeroAmortizacion);
                    query.append(" AND TRANSACCION_INTERNA IN(90110,90111,90460)");
                    query.append(" AND IND_REVERSADA=0");
                    System.out.println(query);
                    resultado = bd.executeQuery(query.toString());
                    if(resultado.next()){
                        if(resultado.getString("INTERES_PAGADO") != null)
                            interesPagado = resultado.getDouble("INTERES_PAGADO");
                    }

                    query.setLength(0);
                    query.append("SELECT ");
                    query.append("SUM(MONTO_TRANSACCION) AS IMPUESTO_PAGADO ");
                    query.append("FROM CRE_HISTORICO_MOVTOS ");
                    query.append("WHERE NUMERO_INSTITUCION= ").append(numeroInstitucion);
                    query.append(" AND NUMERO_CUENTA= ").append(numeroCuenta);
                    query.append(" AND NUMERO_TABLA= ").append(numeroTabla);
                    query.append(" AND NUMERO_AMORTIZACION= ").append(numeroAmortizacion);
                    query.append(" AND TRANSACCION_INTERNA IN(90463,90464)");
                    query.append(" AND IND_REVERSADA=0");
                    System.out.println(query);
                    resultado = bd.executeQuery(query.toString());
                    if(resultado.next()){
                        if(resultado.getString("IMPUESTO_PAGADO") != null)
                            impuestoPagado = resultado.getDouble("IMPUESTO_PAGADO");
                    }
                }

                query.setLength(0);
                query.append("UPDATE ");
                query.append("CRE_TABLA_AMORTIZACIONES ");
                query.append("SET IO_TASA= (");
                query.append("SELECT ");
                query.append("IO_TASA ");
                query.append("FROM CRE_CUENTAS ");
                query.append("WHERE NUMERO_INSTITUCION= ").append(numeroInstitucion);
                query.append(" AND NUMERO_CUENTA= ").append(numeroCuenta);
                query.append("), INTERES_ORDINARIO= ").append(montoInteres);
                query.append(", IMPTO_INTERES_ORDINARIO= ").append(impuestoInteres);
                query.append(", TOTAL_PAGO= ");
                query.append("CAPITAL_AMORTIZADO+ ");
                query.append(montoInteres).append("+ ");
                query.append(impuestoInteres).append("+ ");
                query.append("COMISIONES+ ");
                query.append("IMPTO_COMISIONES+ ");
                query.append("SEGURO+ ");
                query.append("FOMENTO_AHORRO+ ");
                query.append("INTERES_MORATORIO+ ");
                query.append("IMPTO_INTERES_MORATORIO+ ");
                query.append("MONTO_CAPITALIZADO+ ");
                query.append("OTRO_CARGO1+ ");
                query.append("OTRO_CARGO2+ ");
                query.append("OTRO_CARGO3+ ");
                query.append("OTRO_CARGO4+ ");
                query.append("OTRO_CARGO5");
                query.append(", SDO_INTERES_ORDINARIO= ").append(montoInteres - interesPagado);
                query.append(", SDO_IMPTO_INTERES_ORDINARIO= ").append(impuestoInteres - impuestoPagado);
                query.append(", SDO_TOTAL_PAGO= ");
                query.append("SDO_CAPITAL_AMORTIZADO+ (");
                query.append(montoInteres - interesPagado).append(") + (");
                query.append(impuestoInteres - impuestoPagado).append(") + ");
                query.append("SDO_COMISIONES+ ");
                query.append("SDO_IMPTO_COMISIONES+ ");
                query.append("SDO_SEGURO+ ");
                query.append("SDO_FOMENTO_AHORRO+ ");
                query.append("SDO_INTERES_MORATORIO+ ");
                query.append("SDO_IMPTO_INTERES_MORATORIO+ ");
                query.append("SDO_OTRO_CARGO1+ ");
                query.append("SDO_OTRO_CARGO2+ ");
                query.append("SDO_OTRO_CARGO3+ ");
                query.append("SDO_OTRO_CARGO4+ ");
                query.append("SDO_OTRO_CARGO5");
                query.append(", FECHA_MODIFICACION= '").append(fechaActual).append("'");
                query.append(", ID_USUARIO_MODIFICACION= '").append(idUsuario).append("'");
                query.append(", MAC_ADDRESS_MODIFICACION= '").append(macAddress).append("'");
                query.append(" WHERE NUMERO_INSTITUCION= ").append(numeroInstitucion);
                query.append(" AND NUMERO_CUENTA= ").append(numeroCuenta);
                query.append(" AND NUMERO_TABLA= ").append(numeroTabla);
                query.append(" AND NUMERO_AMORTIZACION= ").append(numeroAmortizacion);
                System.out.println(query);
                bd.executeUpdate(query.toString());
            }    
            
            if (isConexionAbierta == 0)
                bd.close();
        } catch (SQLException se) {
            if (isConexionAbierta == 0)
                bd.close();
            se.printStackTrace();
            throw new SQLException("131");
        }    
    }
}
