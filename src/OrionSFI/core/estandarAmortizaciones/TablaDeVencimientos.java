package OrionSFI.core.estandarAmortizaciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import OrionSFI.core.commons.SQLProperties;
import OrionSFI.core.fechaInstitucion.FechaInstitucion;
import OrionSFI.core.estandarFechas.EstandarFechas;
public class TablaDeVencimientos {

    static double PorcentajeImpuesto, MontoExcento, MontoInversion, Tasa, SaldoMinimo, SaldoMaximo;
    static String FechaApertura, BaseCalculoInteres, VencimientoDiaInhabil,
            TipoInteres, CalculoIntInhabil;
    static int MesBase, AñoBase, DiaPago, DiaPago2, FrecuenciaPago, Frecuencia;
    static long Sucursal;
    static char PeriodoPago;
    static boolean PlanInteres;
    static boolean fechaInteres = false;
    static List<String[]> liTabla = new ArrayList<>();

    static SQLProperties sqlProperties = new SQLProperties();
	static String separador = sqlProperties.getSeparador();
    static EstandarFechas estandarFechas = new EstandarFechas();
    static FechaInstitucion fechaInst = new FechaInstitucion();
    private static String fechaProxPagoCap = "";
    
    public static String getFechaProxPagoCap() {
        return fechaProxPagoCap;
    }

    public static void setFechaProxPagoCap(String value) {
        fechaProxPagoCap = value;
    }

    private static String fechaProxPagoInt = "";
    
    public static String getFechaProxPagoInt() {
        return fechaProxPagoInt;
    }

    public static void setFechaProxPagoInt(String value) {
        fechaProxPagoInt = value;
    }

    private static int plazoMeses;
    
    public static int getPlazoMeses() {
        return plazoMeses;
    }

    public static void setPlazoMeses(int value) {
        plazoMeses = value;
    }


    public TablaDeVencimientos(double porImpto, double mntExcento, double mntInv, double tasa,
    		double sldMin, double sldMax, String fApert,
        String baseCalcInt, String vencInhabil, String calcIntInhabil, int mesB,
        int añoB, int diaPago, int diaPago2, int frecPago, int frec, 
        boolean planInt, char perPago, long sucursal)
    {
        PorcentajeImpuesto = porImpto; MontoExcento = mntExcento; MontoInversion = mntInv;
        Tasa = tasa; SaldoMinimo = sldMin; SaldoMaximo = sldMax; FechaApertura = fApert;
        BaseCalculoInteres = baseCalcInt; VencimientoDiaInhabil = vencInhabil;
        CalculoIntInhabil = calcIntInhabil; MesBase = mesB; AñoBase = añoB;
        DiaPago = diaPago; DiaPago2 = diaPago2; FrecuenciaPago = frecPago; 
        PlanInteres = planInt; PeriodoPago = perPago; Sucursal = sucursal;
        Frecuencia = frec;
    }
    
    public static Date GetFecha(String fecha) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");
        return dateFormat.parse(fecha);
    }
    public static LocalDate GetFechaLD(String fecha) throws ParseException {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return  LocalDate.parse(fecha, formatter);
    }
	
	 public static String GetFecha(Date fecha) {
	        int dia = fecha.getDate();
	        int mes = fecha.getMonth() + 1; // En Java, los meses van de 0 a 11, por lo que se suma 1 para obtener el mes correcto
	        int año = fecha.getYear() + 1900; // En Java, getYear() devuelve el año desde 1900, por eso se suma 1900 para tener el año completo

	        StringBuilder sb = new StringBuilder();
	        if (dia < 10) {
	            sb.append("0");
	        }
	        sb.append(dia);
	        sb.append("/");
	        if (mes < 10) {
	            sb.append("0");
	        }
	        sb.append(mes);
	        sb.append("/");
	        sb.append(año);
	        return sb.toString();
	  }
	 
	 public static void SetTabla()
	 {
		 String[] encabezado= {
				 "NUMERO", 
				 "FECHA_PAGO" ,
                 "FECHA_INICIAL" , 
                 "FECHA_FINAL" ,
                 "DIAS" , 
                 "TASA_ANUAL" ,
                 "CAPITAL" ,
                 "CAPITAL_POR_PAGAR" , 
                 "INTERES_BRUTO",
                 "IMPUESTO_RETENIDO" , 
                 "INTERES_NETO" , 
                 "PAGO_TOTAL"};
		 liTabla.add(encabezado);
	 }
	 
	 public static String GetFechaVencimiento(Date fecha) {
		 Calendar calendar = Calendar.getInstance();
	     calendar.setTime(fecha);
	     calendar.add(Calendar.DAY_OF_MONTH, Frecuencia); // Suma la frecuencia en días a la fecha

	     Date fechaVencimiento = calendar.getTime(); // Obtiene la nueva fecha después de agregar la frecuencia
	     return GetFecha(fechaVencimiento);
	 }
	 
	 public static int DiasEntreFechas(Date fecha1,Date fecha2) {
		 return (int) ((fecha2.getTime() - fecha1.getTime()) / 86400000);
	 }
	 
	 public static double GetBaseCapital(double MI) {
		 	BigDecimal valorDecimal = new BigDecimal(MI).setScale(2, RoundingMode.HALF_UP);
	        
	        if (MI>SaldoMaximo) 
	        	 valorDecimal  = new BigDecimal(SaldoMaximo).setScale(2, RoundingMode.HALF_UP);
	        if (MI<SaldoMaximo)  
	            valorDecimal = BigDecimal.ZERO;
	        
	        double numeroRedondeado = valorDecimal.doubleValue();
	        return numeroRedondeado;
	}
	 
	public double GetCapitalPorPagar()
	{
		return MontoInversion;
	}
	
	public static double GetInteresBruto(double BC, double T, int D, int AB) {
		T = T / 100;
		BigDecimal valorDecimal = new BigDecimal((BC * T * D / AB)).setScale(2, RoundingMode.HALF_UP);
		double numeroRedondeado = valorDecimal.doubleValue();
	    return numeroRedondeado;
	}
	
	public static double GetImpuestoRetenido(double BC, double IB, int D) {
		
		double PI = PorcentajeImpuesto / 100;
		double res = (BC - MontoExcento) * PI * D / AñoBase;
		BigDecimal valorDecimal = new BigDecimal(res).setScale(2, RoundingMode.HALF_UP);
        if (res < 0)
            return 0;
        if (res > IB)
        	valorDecimal = new BigDecimal(IB).setScale(2, RoundingMode.HALF_UP);
        
		double numeroRedondeado = valorDecimal.doubleValue();
	    return numeroRedondeado;
	}
	
	public static double GetInteresNeto(double IB, double IR)
    {
        return IB - IR;
    }

	public static double GetDouble(Object valor) {
        return Double.parseDouble(valor.toString());
    }
	
	public static int GetInt(Object valor)
    {
        return Integer.parseInt(valor.toString());
    }
	private static String GetFechaHabil(short numInstitucion,String Fecha, long suc, String Indicador) throws Exception
    {
		
        return fechaInst.getdiaHabilInstitucional(numInstitucion, Fecha, suc,Indicador);
        
    }
	public static String CalcularFechaPrimerPago(LocalDate FechaApertura, String TipoPago,
	        char Periodicidad, boolean indGraciaCapital, boolean indGraciaInteres, int DiaPago1, int DiaPago2,
	        double DiasAntesPrimerVencimiento, int Frecuencia) {
	    LocalDate FechaMinima = LocalDate.now();
	    LocalDate FechaPrimerPago = LocalDate.now();

	    if (!TipoPago.equals("I")) {
	        if (!indGraciaCapital && !indGraciaInteres) {
	            FechaMinima = FechaApertura.plusDays((long) DiasAntesPrimerVencimiento);
	            if (Periodicidad == 'M') {
	                if (FechaMinima.getDayOfMonth() < DiaPago1 && FechaMinima.getDayOfMonth() < FechaMinima.lengthOfMonth())
	                    FechaPrimerPago = FechaMinima.plusMonths(Frecuencia - 1);
	                else
	                    FechaPrimerPago = FechaMinima.plusMonths(Frecuencia);
	                if (DiaPago1 > FechaPrimerPago.lengthOfMonth())
	                    FechaPrimerPago = LocalDate.of(FechaPrimerPago.getYear(), FechaPrimerPago.getMonthValue(),
	                            FechaPrimerPago.lengthOfMonth());
	                else
	                    FechaPrimerPago = LocalDate.of(FechaPrimerPago.getYear(), FechaPrimerPago.getMonthValue(),
	                            DiaPago1);
	            } else if (Periodicidad == 'D') {
	                if (Frecuencia == 15) {
	                    if (FechaMinima.getDayOfMonth() < DiaPago1)
	                        FechaPrimerPago = LocalDate.of(FechaMinima.getYear(), FechaMinima.getMonthValue(), DiaPago1);
	                    else {
	                        if (FechaMinima.getDayOfMonth() < DiaPago2) {
	                            if (FechaMinima.getDayOfMonth() >= FechaMinima.lengthOfMonth()) {
	                                FechaMinima = FechaMinima.plusMonths(1);
	                                FechaPrimerPago = LocalDate.of(FechaMinima.getYear(), FechaMinima.getMonthValue(),
	                                        DiaPago1);
	                            } else if (DiaPago2 > FechaMinima.lengthOfMonth()) {
	                                FechaPrimerPago = LocalDate.of(FechaMinima.getYear(), FechaMinima.getMonthValue(),
	                                        FechaMinima.lengthOfMonth());
	                            } else
	                                FechaPrimerPago = LocalDate.of(FechaMinima.getYear(), FechaMinima.getMonthValue(),
	                                        DiaPago2);
	                        } else {
	                            FechaMinima = FechaMinima.plusMonths(1);
	                            FechaPrimerPago = LocalDate.of(FechaMinima.getYear(), FechaMinima.getMonthValue(),
	                                    DiaPago1);
	                        }
	                    }
	                } else {
	                    FechaPrimerPago = FechaApertura.plusDays(Frecuencia);
	                }
	            } else if (Periodicidad == 'Q') {
	                FechaPrimerPago = FechaApertura.plusDays(Frecuencia);
	            }
	        }
	    }

	    int fAperturaM = FechaApertura.getMonthValue();
	    int fechaPPM = FechaPrimerPago.getMonthValue();

	    int fAperturaD = FechaApertura.getDayOfMonth();
	    String fechaCorregida = "";
	    String fechaPP = "";
	    LocalDate fechaStandar = LocalDate.now();
	    int fechaPPD = 0;
	    /*if (Serial != null) {
	        fechaCorregida = RegresaFechaCorrectaConDiagonal(FechaPrimerPago.getDayOfMonth(),
	                FechaPrimerPago.getMonthValue(), FechaPrimerPago.getYear());
	        fechaPP = GetFechaHabil(fechaCorregida);
	        fechaStandar = ObtieneFechaStandard(fechaPP);
	        fechaPPD = fechaStandar.getDayOfMonth();
	        fechaPPM = fechaStandar.getMonthValue();
	    } else {*/
	        fechaPPD = FechaPrimerPago.getDayOfMonth();
	    //}

	    if (FechaPrimerPago.equals(LocalDate.MIN))
	        return RegresaFechaCorrectaConDiagonal(FechaPrimerPago.getDayOfMonth(), FechaPrimerPago.getMonthValue(),
	                FechaPrimerPago.getYear());
	    else if (Periodicidad == 'M' && fAperturaM == fechaPPM && fAperturaD == fechaPPD && !TipoPago.equals("C")) {
	        FechaPrimerPago = FechaMinima.plusMonths(Frecuencia);
	        if (DiaPago1 == 31)
	            DiaPago1 = FechaPrimerPago.lengthOfMonth();
	        FechaPrimerPago = LocalDate.of(FechaPrimerPago.getYear(), FechaPrimerPago.getMonthValue(), DiaPago1);
	        return RegresaFechaCorrectaConDiagonal(FechaPrimerPago.getDayOfMonth(), FechaPrimerPago.getMonthValue(),
	                FechaPrimerPago.getYear());
	    } else {
	        return RegresaFechaCorrectaConDiagonal(FechaPrimerPago.getDayOfMonth(), FechaPrimerPago.getMonthValue(),
	                FechaPrimerPago.getYear());
	    }
	}
	public static String RegresaFechaCorrectaConDiagonal(int dia, int mes, int año) {
        String diaString = (dia < 10) ? "0" + dia : String.valueOf(dia);
        String mesString = (mes < 10) ? "0" + mes : String.valueOf(mes);
        String añoString = String.valueOf(año);
        return diaString + "/" + mesString + "/" + añoString;
    }
	  public static int obtenerMesesEntreDosFechas(LocalDate fechaFinal, LocalDate fechaInicial) {
	        if (fechaFinal.isBefore(fechaInicial)) {
	            throw new IllegalArgumentException("La Fecha Inicial debe ser menor o igual a la Fecha Final");
	        }
	        
	        long meses = ChronoUnit.MONTHS.between(fechaInicial, fechaFinal);
	        return (int) meses;
	    }

	public static void LlenaTabla() throws Exception{
		String FechaTmp;
		
		int Numero =0;
		String FechaPago = "";
		String FechaInicial = "";
		String FechaFinal = "";
		int Dias = 0;
		double TasaAnual = 0;
		double Capital = 0;
		double CapitalPorPagar = 0;
		double InteresBruto = 0;
		double ImpuestoRetenido = 0;
		double InteresNeto = 0;
		double PagoTotal = 0; 
		
		 if (!PlanInteres)
         {
			 FechaInicial =FechaApertura;
			 FechaTmp = GetFechaVencimiento(GetFecha(FechaApertura));
			 if (VencimientoDiaInhabil == "V")
				 FechaFinal = FechaTmp;
			 else
				 FechaFinal = GetFechaHabil((short)1, FechaTmp, Sucursal, VencimientoDiaInhabil);
			 if (CalculoIntInhabil == "FV")
				 FechaPago = FechaTmp;
             else
            	 FechaPago= FechaFinal;
            //LlenaCampos(ref Fila);
			 double dcTmp;
			 Numero = (liTabla.size());
			 Dias = DiasEntreFechas(GetFecha(FechaFinal),
	         GetFecha(FechaInicial));
			 TasaAnual = Tasa;
	         Capital = GetBaseCapital(MontoInversion);
	            dcTmp = GetInteresBruto(Capital,
	            		TasaAnual, Dias, AñoBase);
	            if (dcTmp != 0 && !fechaInteres)
	            {
	                fechaProxPagoInt = FechaPago;
	                fechaInteres = true;
	            }

	            InteresBruto = dcTmp;
	            ImpuestoRetenido = GetImpuestoRetenido(Capital,
	                InteresBruto, Dias);
	            InteresNeto = GetInteresNeto(InteresBruto,
	                ImpuestoRetenido);
			 //Fin LlenaCampos
	         CapitalPorPagar = MontoInversion;
             PagoTotal = CapitalPorPagar + InteresNeto;
             fechaProxPagoCap = FechaPago;
             String[] datos= {
            		 Numero +"" ,
            		 FechaPago,
                     FechaInicial , 
                     FechaFinal ,
                     Dias+"" , 
                     TasaAnual + "" ,
                     Capital +"" ,
                     CapitalPorPagar+"" , 
                     InteresBruto+"" ,
                     ImpuestoRetenido+"" , 
                     InteresNeto+"", 
                     PagoTotal+""
             };
             liTabla.add(datos);
         }
		 else{
            Date FV = GetFecha(GetFechaVencimiento(GetFecha(FechaApertura)));
            Date FF = new Date(0);
            Date FP = new Date(0);

            while (FF.before(FV))
            {
            	if (liTabla.size() == 1)
                {
                    FechaInicial= FechaApertura;
                    FechaTmp = CalcularFechaPrimerPago(GetFechaLD(FechaInicial), "", PeriodoPago, false, false,DiaPago, DiaPago2, 0, FrecuenciaPago);
                    
                    if (GetFecha(FechaTmp).after(FV))
                        FechaTmp = GetFecha(FV);
                    if (VencimientoDiaInhabil == "V")
                        FechaFinal = FechaTmp;
                    else
                    	FechaFinal = GetFechaHabil((short) 1, FechaTmp, Sucursal, VencimientoDiaInhabil);
                }
            	else
                {	
            		String[] FilaAnterior = liTabla.get(liTabla.size()-1);
                   FechaInicial = FilaAnterior[3];
                   FechaTmp = estandarFechas.calcularFechaSiguiente(
                        FechaInicial, PeriodoPago, DiaPago, DiaPago2,
                        FrecuenciaPago);
                    if (VencimientoDiaInhabil == "V")
                        FechaFinal= FechaTmp;
                    else
                    	FechaFinal = GetFechaHabil((short)1, FechaTmp, Sucursal, VencimientoDiaInhabil);
                }
                if (CalculoIntInhabil == "FV")
                    FechaPago = FechaTmp;
                else
                    FechaPago = FechaFinal;
                //LlenaCampos
                double dcTmp;
	   			 Numero = (liTabla.size());
	   			 Dias = DiasEntreFechas(GetFecha(FechaFinal),
	   	         GetFecha(FechaInicial));
	   			 TasaAnual = Tasa;
	   	         Capital = GetBaseCapital(MontoInversion);
   	            dcTmp = GetInteresBruto(Capital,
   	            		TasaAnual, Dias, AñoBase);
   	            if (dcTmp != 0 && !fechaInteres)
   	            {
   	                fechaProxPagoInt = FechaPago;
   	                fechaInteres = true;
   	            }

   	            InteresBruto = dcTmp;
   	            ImpuestoRetenido = GetImpuestoRetenido(Capital,
   	                InteresBruto, Dias);
   	            InteresNeto = GetInteresNeto(InteresBruto,
   	                ImpuestoRetenido);
                //FinLlenaCampos

                CapitalPorPagar = 0;
                PagoTotal = CapitalPorPagar +InteresNeto;
                FF = GetFecha(estandarFechas.calcularFechaSiguiente(
                    FechaPago, PeriodoPago, DiaPago, DiaPago2,
                    FrecuenciaPago));
                FP = GetFecha(FechaPago);
                String[] datos= {
               		 Numero +"" ,
               		 FechaPago,
                        FechaInicial , 
                        FechaFinal ,
                        Dias+"" , 
                        TasaAnual + "" ,
                        Capital +"" ,
                        CapitalPorPagar+"" , 
                        InteresBruto+"" ,
                        ImpuestoRetenido+"" , 
                        InteresNeto+"", 
                        PagoTotal+""
                };
                liTabla.add(datos);
            }
            if (FF.compareTo(FV) >= 0 && !FP.equals(FV))
            {
             
            	String[] FilaAnterior = liTabla.get(liTabla.size()-1);
                FechaInicial = FilaAnterior[3];
                FechaTmp = GetFecha(FV);
                if (VencimientoDiaInhabil == "V")
                    FechaFinal= FechaTmp;
                else
                	FechaFinal = GetFechaHabil((short)1, FechaTmp, Sucursal, VencimientoDiaInhabil);
                if (CalculoIntInhabil == "FV")
                    FechaPago= FechaTmp;
                else
                	FechaPago = FechaFinal;
                	
                //LlenaCampos(ref Fila);
                double dcTmp;
	   			 Numero = (liTabla.size());
	   			 Dias = DiasEntreFechas(GetFecha(FechaFinal),
	   	         GetFecha(FechaInicial));
	   			 TasaAnual = Tasa;
	   	         Capital = GetBaseCapital(MontoInversion);
   	            dcTmp = GetInteresBruto(Capital,
   	            		TasaAnual, Dias, AñoBase);
   	            if (dcTmp != 0 && !fechaInteres)
   	            {
   	                fechaProxPagoInt = FechaPago;
   	                fechaInteres = true;
   	            }

   	            InteresBruto = dcTmp;
   	            ImpuestoRetenido = GetImpuestoRetenido(Capital,
   	                InteresBruto, Dias);
   	            InteresNeto = GetInteresNeto(InteresBruto,
   	                ImpuestoRetenido);
                //Fin LlenaCampos
                	
                CapitalPorPagar = MontoInversion;
                PagoTotal = CapitalPorPagar + InteresNeto;
                fechaProxPagoCap = FechaPago.toString();
                String[] datos= {
                  		 Numero +"" ,
                  		 FechaPago,
                           FechaInicial , 
                           FechaFinal ,
                           Dias+"" , 
                           TasaAnual + "" ,
                           Capital +"" ,
                           CapitalPorPagar+"" , 
                           InteresBruto+"" ,
                           ImpuestoRetenido+"" , 
                           InteresNeto+"", 
                           PagoTotal+""
                   };
                   liTabla.add(datos);
            }
            else
            {
            	String[] FilaAnterior = liTabla.get(liTabla.size()-1);
                String FechaPagoAnterior = FilaAnterior[3];
                fechaProxPagoCap = FechaPagoAnterior;
                double nCapitalPorPagar = MontoInversion;
                double nPagoTotal =
                    Double.parseDouble(FilaAnterior[7]) + 
                    Double.parseDouble(FilaAnterior[10]);
                String[] FilaActualizada = {
                		FilaAnterior[0],
                		FilaAnterior[1],
                		FilaAnterior[2],
                		FilaAnterior[3],
                		FilaAnterior[4],
                		FilaAnterior[5],
                		FilaAnterior[6],
                		nCapitalPorPagar + "",
                		FilaAnterior[8],
                		FilaAnterior[9],
                		FilaAnterior[10],
                		nPagoTotal + ""
                };
                liTabla.set(liTabla.size(), FilaActualizada);
            }
         }
		  plazoMeses = obtenerMesesEntreDosFechas(
          		GetFechaLD(GetFechaVencimiento(GetFecha(FechaApertura))), GetFechaLD(FechaApertura));
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Date fecha = GetFecha("07/05/2023");
		//System.out.println(fecha.toString());
		//String fechaf = getFecha(fecha);
		//System.out.println(fechaf);
		
		TablaDeVencimientos tab = new TablaDeVencimientos(30.5,13.00,44.0, 1.2,
	    		0.5 , 7.1, "12/05/2023",
	            "3", "V", "FV", 6,
	            1998, 28, 30, 4, 222, 
	            true, 'D', (long)1);
		//System.out.println(tab.getFechaVencimiento(fecha));
		//System.out.println(diasEntreFechas(fecha, getFecha(tab.getFechaVencimiento(fecha))));
		//System.out.println(getBaseCapital((double)5.9264));
		 //Object valor1 = 123; // Valor de ejemplo
	     //Object valor2 = "67"; // Valor de ejemplo
	     
	     //System.out.println(GetInt(valor1));
	     //System.out.println(GetInt(valor2));
		//System.out.println(obtenerMesesEntreDosFechas(GetFechaLD("12/05/2023"),GetFechaLD("12/05/1994")));
		tab.SetTabla();
		tab.LlenaTabla();
		//tab.LlenaTabla();
		//tab.LlenaTabla();
		for (String[] array : tab.liTabla) {
            System.out.println(Arrays.toString(array));
        }
	}

}
