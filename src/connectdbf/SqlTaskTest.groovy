package connectdbf

import org.joda.time.DateTime

/**
 * Created by 1 on 17.08.2016.
 */
class SqlTaskTest extends GroovyTestCase {

    @Override
    void tearDown() {


        def connect = SqlTask.connectCurrent;

        if (connect) {
            SqlTask.connectCurrent.close();
        }

        super.tearDown();
    }

    void setUp() {


        String user_dir = System.getProperty("user.dir");

        def a = user_dir.tokenize('\\');

        def last = a[-1];

        a.remove(last);
        user_dir = a.join('\\');

        def connectionDbf = SqlTask.openTestBase(user_dir, "KlientsBase");

        SqlTask.connectCurrent = connectionDbf;



        super.setUp()

    }

    void testGetTabMonitor() {

        //  def dtFirst = cmd.getProperty("dateTimeFirst");
        //  def dtLast = cmd.getProperty("dateTimeLast");

        // def id = property.id_object as Integer;

        //   def tFirst = new Timestamp(dtFirst.getMillis());
        // def tLast = new Timestamp(dtLast.getMillis());
        def id = 92;

        def map = [:];

        def sql = "SELECT  energy_down_0_0,value_date FROM enegry_data WHERE id_object=? AND tarif=?";

        //sql = "SELECT  energy_down_0_0 FROM enrgry_data WHERE id_object=? AND value_date>=? AND value_date<=?";


        def os = [id, 0];


        def rsValues = SqlTask.getResultSet(null, sql, os);


        try {
            while (rsValues.next()) {

                def alVal = [];

                def timestamp = rsValues.getTimestamp("value_date");
                def val = rsValues.getDouble("energy_down_0_0");
                DateTime time = new DateTime(timestamp.time);
                def mont = time.monthOfYear;
                def day = time.dayOfMonth;
                def hour = time.hourOfDay;

                def stime = "${mont}_${day}_${hour}".toString();
                map.put(stime, val);

            }
        } finally {
            rsValues.close();
        }

        assertNotNull(map);
        //  return  map;


    }


    void testGetTabValues() {


        def objval = "";

        //DateTime dateTime = MathTrans.getDateReport(par4);


        def dtFirst = new DateTime().millisOfDay().setCopy(0);
        dtFirst = dtFirst.monthOfYear().setCopy(7);
        dtFirst = dtFirst.dayOfMonth().setCopy(1);

        def dtLast = new DateTime().millisOfDay().setCopy(0);
        dtLast = dtLast.monthOfYear().setCopy(7);
        dtLast = dtLast.dayOfMonth().setCopy(31);





        def map = SqlTask.getObjectValues(null, "profil_power", 93, dtFirst, dtLast);



        assertNotNull(map);

    }

    //OBJ_PARAM()
    void testCmd1() {


    }
}
