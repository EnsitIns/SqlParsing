/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package connectdbf;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * @author Сычев С.А.
 */
public class SqlTask {

    public static final String CONN_JDBC = "jdbc"; // драйвер  подключения
    public static final String CONN_NAME_BASE = "databaseURL"; // путь к вазе
    public static final String CONN_NEW_BASE = "newBase"; // новая база
    public static org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger("LogServer");
    public static final int B_LITTLE_ENDIAN = 0;  // от Младшего к старшему
    public static final int B_BIG_ENDIAN = 1;  // от старшего  к младшему
    public static HashMap<String, String> HM_VIEWS = new HashMap<String, String>(); //Представления
    public static Connection connectCurrent; //Текущее соединение
    public static Locale localeCurrent; //Текущая локализация

    /**
     * Отсортированный неповторяющися список по полю
     *
     * @param connection
     * @param name       -поле выбора
     * @param nameTable- таблица
     * @return
     * @throws SQLException
     */
    public static ArrayList<Object> getDistinctCaptionByName(Connection connection, String name, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ArrayList<Object> result = new ArrayList<Object>();

        String sql = "SELECT DISTINCT " + name + " FROM " + nameTable + " ORDER BY " + name;

        ResultSet rs = getResultSet(connection, sql);

        try {

            while (rs.next()) {

                Object label = rs.getObject(name);

                if (label != null) {
                    result.add(label);
                }
            }

        } finally {
            rs.getStatement().close();
        }

        return result;
    }

    /**
     * Возвращает карту параметров таблицы по его ключу/ключам (primary key)
     *
     * @param connection
     * @param nameTable
     * @param keys
     * @return
     * @throws java.sql.SQLException
     */
    public static HashMap<String, Object> getValuesByKey(Connection connection, String nameTable, Object[] keys) throws SQLException {

        ResultSet rs;

        String sql;

        if (connection == null) {
            connection = connectCurrent;
        }
        ArrayList<String> aKeys = getPrimaryKey(connection, nameTable);

        if (aKeys.size() != keys.length) {
            throw new SQLException("количество ключей и значений не совпадает !");
        }

        HashMap<String, Object> hm = new HashMap<String, Object>();

        sql = "SELECT * FROM " + nameTable + " WHERE ";

        StringBuilder builder = new StringBuilder(sql);

        for (String sKey : aKeys) {

            builder.append(sKey);
            builder.append("=?");

            int idx = aKeys.indexOf(sKey);
            if (idx < aKeys.size() - 1) {
                builder.append(" AND ");
            }
        }
        sql = builder.toString();
        rs = SqlTask.getResultSet(connection, sql, keys);

        try {
            if (rs.next()) {
                addParamToMap(rs, hm);
            }
        } finally {
            rs.close();
        }

        return hm;
    }

    /**
     * Возвращает значение по строковому значению
     *
     * @param connection
     * @param hmValues
     * @param nameTable
     * @throws java.sql.SQLException
     */
    public static void getValuesByString(Connection connection, HashMap<String, Object> hmValues, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        HashMap<String, Integer> hmValTyp;

        hmValTyp = getNameAndTypeCol(connection, nameTable);

        for (String nameValue : hmValues.keySet()) {

            String sVal = (String) hmValues.get(nameValue);

            Object object = getValueByString(connection, hmValTyp, nameTable, nameValue, sVal);

        }

    }


    /**
     * Возвращает значение по строковому значению
     *
     * @param connection
     * @param hmValTyp
     * @param nameTable
     * @param nameValue
     * @param value
     * @return
     * @throws java.sql.SQLException
     */
    public static Object getValueByString(Connection connection, HashMap<String, Integer> hmValTyp, String nameTable, String nameValue, String value) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        Object result = null;

        if (hmValTyp == null) {
            hmValTyp = getNameAndTypeCol(connection, nameTable);
        }
        int iTyp;

        if (!hmValTyp.containsKey(nameValue)) {

            throw new SQLException(nameValue);
        }
        iTyp = hmValTyp.get(nameValue);

        try {
            if (iTyp == java.sql.Types.BIGINT) {
                result = Long.parseLong(value);
            } else if (iTyp == java.sql.Types.BIT) {
                result = Boolean.parseBoolean(value);
            } else if (iTyp == java.sql.Types.BINARY) {
            } else if (iTyp == java.sql.Types.BLOB) {
            } else if (iTyp == java.sql.Types.BOOLEAN) {

                result = Boolean.parseBoolean(value);
            } else if (iTyp == java.sql.Types.CHAR || iTyp == java.sql.Types.VARCHAR || iTyp == java.sql.Types.LONGNVARCHAR) {

                result = value;
            } else if (iTyp == java.sql.Types.NUMERIC || iTyp == java.sql.Types.DECIMAL) {

                result = value;
            } else if (iTyp == java.sql.Types.TINYINT) {

                result = Byte.parseByte(value);
            } else if (iTyp == java.sql.Types.SMALLINT) {

                result = Short.parseShort(value);
            } else if (iTyp == java.sql.Types.INTEGER) {

                result = Integer.parseInt(value);
            } else if (iTyp == java.sql.Types.REAL) {

                result = Float.parseFloat(value);
            } else if (iTyp == java.sql.Types.FLOAT || iTyp == java.sql.Types.DOUBLE) {

                result = Double.parseDouble(value);
            } else if (iTyp == java.sql.Types.DATE) {
                result = java.sql.Date.valueOf(value);
            } else if (iTyp == java.sql.Types.TIME) {
                result = java.sql.Time.valueOf(value);
            } else if (iTyp == java.sql.Types.TIMESTAMP) {
                result = java.sql.Timestamp.valueOf(value);
            } else {

                throw new SQLException("Тип" + iTyp + " не поддерживается ");
            }

        } catch (NumberFormatException ex) {

            throw new SQLException(nameValue, ex);
        }

        return result;
    }

    /**
     * Возвращает имена и типы таблиц без представлений
     *
     * @param connection
     * @return
     * @throws java.sql.SQLException
     */
    public static HashMap<String, Integer> getNamesAndTypeTablesXML(Connection connection) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        String sql;

        sql = "SELECT c_name_table,c_obj_type FROM c_obj_spec WHERE c_obj_type<30";

        HashMap<String, Integer> result = new HashMap<String, Integer>();

        ResultSet resultSet = getResultSet(connection, sql);

        try {
            while (resultSet.next()) {
                String name = resultSet.getString("c_name_table");
                Integer type = resultSet.getInt("c_obj_type");
                result.put(name, type);
            }

        } finally {
            resultSet.getStatement().close();
        }

        return result;
    }

    public static Integer createNewBaseDialog(String nameBase, Component component) {

        Integer result = -1;

        // Такой базы нет
        String msg = "<html><h4>Базы данных с именем :" + nameBase + "</h4>"
                + "<h3>не существует !</h3>"
                + "<h3><FONT COLOR=#ff0000>Создать новую базу ?</FONT><h3></html>";

        String[] sv = {"Создать", "Выход"};

        result = JOptionPane.showOptionDialog(component, msg, "Ошибка доступа", JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null, sv, "Выход");

        return result;
    }

    /**
     * Текущее название представления таблицы если нет то название таблицы
     *
     * @param nameTable
     * @return
     */
    public static String getViewByTable(String nameTable) {

        String result = nameTable;

        if (HM_VIEWS != null && HM_VIEWS.containsKey(nameTable)) {

            result = HM_VIEWS.get(nameTable);
        }

        return result;
    }

    /**
     * Возвращает листинг всех поддерживаемых типов текущей базой данных
     */
    public static ArrayList<String> getTypInfo(Connection connection) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ArrayList<String> result = new ArrayList<>();

        DatabaseMetaData dbmt;

        dbmt = connection.getMetaData();
        ResultSet rs;
        rs = dbmt.getTypeInfo();

        try {

            while (rs.next()) {

                String types = rs.getString("TYPE_NAME");

                result.add(types);
            }
        } finally {
            rs.close();
        }

        return result;

    }

    /**
     * Проверяет может ли быть числом строка
     *
     * @param value
     * @return
     */
    public static boolean isNumber(String value) {

        boolean result = true;

        try {
            Double d = Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            result = false;
        }
        return result;
    }

    /**
     * Возвращает строку между двумя символами
     */
    public static String getDelimitedString(String from, char start, char end) {

        int startPos = from.indexOf(start);
        int endPos = from.lastIndexOf(end);

        if (startPos > endPos) {
            return null;
        } else if (startPos == -1) {
            return null;
        } else if (endPos == -1) {
            return from.substring(startPos);
        } else {
            return from.substring(startPos + 1, endPos);
        }

    }

    public static ArrayList<String> getStringNames(String format) {

        ArrayList<String> result = new ArrayList<String>();
        String SetChar = "1234567890@&abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_";

        boolean bYes = false;

        String name = "";

        for (int i = 0; i < format.length(); i++) {

            char c = format.charAt(i);

            if (SetChar.indexOf(c) != -1) {

                name = name + c;

                bYes = true;

            } else {

                if (bYes) {

                    result.add(name);
                    name = "";
                    bYes = false;
                }
            }
        }

        if (!name.isEmpty()) {
            result.add(name);
        }

        return result;
    }

    /**
     * Разбор строки типа А=0; B=1; c=2
     *
     * @param nameCmd
     * @return
     */
    public static HashMap<String, String> getMapNameCmd(String nameCmd) {

        HashMap<String, String> result = new HashMap<String, String>();
        String[] ses = nameCmd.split(";");
        for (String ss : ses) {
            String[] ses1 = ss.split("=");
            result.put(ses1[0], ses1[1]);
        }
        return result;
    }

    /**
     * Разбор строки типа А=0; B=1; c=2
     *
     * @param nameCmd
     * @param typGet  0-key 1-value
     * @return
     */
    public static ArrayList<String> getListNameCmd(String nameCmd, int typGet) {

        ArrayList<String> result = new ArrayList<String>();
        String[] ses = nameCmd.split(";");
        for (String ss : ses) {
            String[] ses1 = ss.split("=");

            if (typGet == 0) {
                result.add(ses1[0]);

            } else {
                result.add(ses1[1]);
            }

        }
        return result;
    }

    /**
     * Карта отсортированного столбца nameName и столбца nameValue в виде
     * nameName=nameValue
     *
     * @param nameTable -название таблицы
     * @param nameName  ключ
     * @param nameValue значение
     * @return карту в виде nameName=nameValue
     * @throws SQLException
     */
    public static TreeMap<String, Object> getSortMapCaption(Connection connection, String nameTable, String nameName, String nameValue) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        TreeMap<String, Object> result = new TreeMap<String, Object>();
        String sql = "SELECT " + nameName + "," + nameValue + " FROM " + nameTable;

        ResultSet rsCaption = null;

        String name;
        Object value;

        rsCaption = getResultSet(connection, sql);
        try {

            while (rsCaption.next()) {
                name = rsCaption.getString(nameName);
                value = rsCaption.getObject(nameValue);
                result.put(name, value);

            }

        } finally {
            rsCaption.close();

        }

        return result;
    }

    /**
     * Возвращает строки по разделителю
     *
     * @param delim -символы раэделения строк
     */
    public static ArrayList<String> getListByDelim(String string, String delim) {
        ArrayList<String> arrayList = new ArrayList<String>();

        String a[] = string.split(delim);
        for (String ss : a) {
            arrayList.add(ss.trim());
        }
        return arrayList;
    }

    /**
     * Возвращает строки только те, у которых первые символы starts
     *
     * @param delim -символы раэделения строк
     */
    public static ArrayList<String> getListByStartsWith(String string, String starts, String delim) {

        ArrayList<String> arrayList = new ArrayList<String>();

        String a[] = string.split(delim);

        for (String ss : a) {

            if (ss.trim().startsWith(starts)) {

                String sval = ss.trim().substring(starts.length(), ss.trim().length());

                arrayList.add(sval);

            }

        }

        return arrayList;

    }

    /**
     * список задейственных полей в SQL запросе и их названия
     *
     * @param sql
     * @param nameTable
     * @return список задейственных полей в SQL запросе
     */
    public static HashMap<String, Integer> getStatementMap(Connection connection, String sql, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        HashMap<String, Integer> hmCol = new HashMap<String, Integer>();

        HashMap<String, Integer> hm = getNameAndTypeCol(connection, nameTable);

        int idx = sql.indexOf("WHERE");

        if (idx == -1) {

            idx = sql.indexOf("where");

        }

        if (idx == -1) {
            return hmCol;
        }

        String sqlFrom = sql.substring(idx);

        for (int i = 0; i < sqlFrom.length(); i++) {

            String sStart = sqlFrom.substring(i);

            for (String s : hm.keySet()) {

                if (sStart.startsWith(s)) {

                    int typ = hm.get(s);

                    hmCol.put(s, typ);
                }
            }
        }
        return hmCol;
    }

    /**
     * список задейственных полей в SQL запросе
     *
     * @param sql
     * @param nameTable
     * @return список задейственных полей в SQL запросе
     */
    public static ArrayList<String> getStatementList(Connection connection, String sql, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ArrayList<String> alCol = new ArrayList<String>();

        HashMap<String, Integer> hm = getNameAndTypeCol(connection, nameTable);

        int idx = sql.indexOf("WHERE");

        if (idx == -1) {

            idx = sql.indexOf("where");

        }

        if (idx == -1) {
            return alCol;
        }

        String sqlFrom = sql.substring(idx);

        for (int i = 0; i < sqlFrom.length(); i++) {

            String sStart = sqlFrom.substring(i);

            for (String s : hm.keySet()) {

                if (sStart.startsWith(s)) {

                    alCol.add(s);
                }
            }
        }
        return alCol;
    }

    /**
     * Заменяет записи в таблице
     *
     * @param nameTable
     * @param hmCol     карта записываемых столбцов в виде имя-значение
     */
    public static void replaceRecInTable(Connection connection, String nameTable, HashMap<String, Object> hmKeys, HashMap<String, Object> hmCol) throws Exception {

        if (connection == null) {
            connection = connectCurrent;
        }

        StringBuilder sbValues = new StringBuilder();
        PreparedStatement ps = null;
        String sql = "";

        if (hmCol == null || hmCol.isEmpty()) {
            return;
        }

        StringBuilder sbWhere = new StringBuilder();

        sbWhere.append(" WHERE ");

        for (String k : hmKeys.keySet()) {
            sbWhere.append(k);
            sbWhere.append("=?");
            sbWhere.append(" AND ");
        }

        int idx = sbWhere.lastIndexOf("AND");

        sbWhere.delete(idx, idx + 3);

        sbValues.append("UPDATE ");
        sbValues.append(nameTable);
        sbValues.append(" ");
        sbValues.append("SET ");

        for (String ncol : hmCol.keySet()) {

            sbValues.append(ncol);
            sbValues.append("=?,");

        }
        sbValues.deleteCharAt(sbValues.lastIndexOf(","));

        sbValues.append(" ");
        sbValues.append(sbWhere.toString());

        sql = sbValues.toString();

        ps = connection.prepareStatement(sql);

        int i = 1;

        for (String ncol : hmCol.keySet()) {
            Object val = hmCol.get(ncol);

            if (val instanceof File) {
                File file = (File) val;

                FileInputStream fis = new FileInputStream(file);
                ps.setBinaryStream(i, fis, file.length());
                //  fis.close();

            } else {
                ps.setObject(i, val);
            }
            i++;
        }

        // добавляем условие
        for (Object v : hmKeys.values()) {
            ps.setObject(i, v);
            i++;
        }

        int iReplace = ps.executeUpdate();

        try {

            // Если не обновил, значит новая запись
            if (iReplace < 1) {

                insertRecInTable(connection, nameTable, hmCol);

            }

        } finally {

            ps.close();
        }

    }

    /**
     * Заменяет записи в таблице
     *
     * @param nameTable
     * @param hmCol     карта записываемых столбцов в виде имя-значение
     */
    public static void replaceRecInTable(Connection connection, String nameTable, HashMap<String, Object> hmCol, boolean bInsert) throws Exception {

        if (connection == null) {
            connection = connectCurrent;
        }

        StringBuilder sbValues = new StringBuilder();
        PreparedStatement ps = null;
        ArrayList<String> alKeys;

        String sql = "";

        if (hmCol == null || hmCol.isEmpty()) {
            return;
        }

        alKeys = getPrimaryKey(connection, nameTable);

        if (alKeys.isEmpty()) {

            throw new NullPointerException(nameTable + "-Нет главного ключа!");
        }

        for (String s : alKeys) {

            if (!hmCol.containsKey(s)) {

                throw new NullPointerException(nameTable + "- нет ключа " + s);
            }

        }

        StringBuilder sbWhere = new StringBuilder();

        sbWhere.append(" WHERE ");

        for (String k : alKeys) {
            sbWhere.append(k);
            sbWhere.append("=?");
            sbWhere.append(" AND ");
        }

        int idx = sbWhere.lastIndexOf("AND");

        sbWhere.delete(idx, idx + 3);

        sbValues.append("UPDATE ");
        sbValues.append(nameTable);
        sbValues.append(" ");
        sbValues.append("SET ");

        for (String ncol : hmCol.keySet()) {

            sbValues.append(ncol);
            sbValues.append("=?,");

        }
        sbValues.deleteCharAt(sbValues.lastIndexOf(","));

        sbValues.append(" ");
        sbValues.append(sbWhere.toString());

        sql = sbValues.toString();

        ps = connection.prepareStatement(sql);

        int i = 1;

        for (String ncol : hmCol.keySet()) {
            Object val = hmCol.get(ncol);

            if (val instanceof File) {
                File file = (File) val;

                FileInputStream fis = new FileInputStream(file);
                ps.setBinaryStream(i, fis, file.length());
                fis.close();

            } else {
                ps.setObject(i, val);
            }
            i++;
        }

        // добавляем условие
        for (String k : alKeys) {

            Object whe = hmCol.get(k);

            ps.setObject(i, whe);
            i++;
        }

        int iReplace = ps.executeUpdate();

        try {
            // Если не обновил, значит новая запись

            if (iReplace < 1 && bInsert) {

                insertRecInTable(connection, nameTable, hmCol);

            }

        } finally {

            ps.close();
        }

    }

    /**
     * Обновляет записи в таблице
     *
     * @param nameTable
     * @param sqlString Условие вставки
     * @param hmCol     карта записываемых столбцов в виде имя-значение
     */
    public static void updateRecInTable(Connection connection, String nameTable, String sqlString, HashMap<String, Object> hmCol) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        StringBuilder sb = new StringBuilder();
        PreparedStatement ps = null;
        String sql = "";
        if (hmCol == null || hmCol.isEmpty()) {
            return;
        }

        sb.append("UPDATE ");
        sb.append(nameTable);
        sb.append(" ");
        sb.append("SET ");

        for (String ncol : hmCol.keySet()) {
            sb.append(ncol);
            sb.append("=?");

            sb.append(", ");

        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        if (sqlString != null) {

            // Добавляем условие вставки
            sb.append(" ");
            sb.append(sqlString);
        }

        sql = sb.toString();
        ps = connection.prepareStatement(sql);

        int i = 1;

        for (String ncol : hmCol.keySet()) {
            Object val = hmCol.get(ncol);

            if (val instanceof File) {
                File file = (File) val;

                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    throw new SQLException(ex.getMessage());
                }
                ps.setBinaryStream(i, fis, file.length());
                try {
                    fis.close();
                } catch (IOException ex) {
                    throw new SQLException(ex.getMessage());
                }

            } else {
                ps.setObject(i, val);
            }
            i++;
        }

        ps.executeUpdate();
        try {
        } finally {

            ps.close();
        }

    }

    /**
     *  добавляет значения текущей строки базы в карту
     * @param rs текущая строкабазы
     * @param hmProp карта
     * @throws SQLException
     */
    public static void addParamToMap(ResultSet rs, Map<String, Object> hmProp) throws SQLException {

        ResultSetMetaData rsmd = rs.getMetaData();

        if (hmProp == null) {

            throw new SQLException("hmProp==null!");
        }

        for (int i = 1; i <= rsmd.getColumnCount(); i++) {

            String nameCol = rsmd.getColumnName(i).toLowerCase();

            Object objVal = rs.getObject(nameCol);

            if (objVal instanceof Blob) {
                Blob blob = (Blob) objVal;
                byte[] bs = blob.getBytes(1, (int) blob.length());
                objVal = bs;

            }

            if (hmProp.containsKey(nameCol)) {
                // Если такое поле уже есть
                String nameTab = rsmd.getTableName(i).toLowerCase();
                hmProp.put(nameTab + "." + nameCol, objVal);
            } else {

                hmProp.put(nameCol, objVal);

            }

        }

    }

    /**
     * Возвращает имена столбцов таблицы
     * @param nameTable -имя таблицы
     * @return список имен
     */
    public static ArrayList<String> getNamesCol(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ArrayList<String> alCol = new ArrayList<String>();

        DatabaseMetaData data;
        data = connection.getMetaData();

        nameTable = getNameTableByTypBase(connection, nameTable);

        ResultSet resultSet = data.getColumns(null, null, nameTable, null);

        try {
            while (resultSet.next()) {

                String nameCol = resultSet.getString(4).toLowerCase();

                alCol.add(nameCol);
            }
        } finally {
            resultSet.close();
        }

        return alCol;
    }

    /**
     * Возвращает максимальное значение ключа плюс 1
     *
     * @param nameTable -имя таблицы
     * @return значение ключа
     */
    public static Integer getMaxKeyByNameTable(Connection connection, String nameTable) throws SQLException {

        Integer result = null;

        if (connection == null) {
            connection = connectCurrent;
        }
        ArrayList<String> alKeys = getPrimaryKey(connection, nameTable);

        String nameKey = alKeys.get(0);

        String sql = "SELECT max(" + nameKey + ") from " + nameTable;

        ResultSet rs = getResultSet(connection, sql);

        try {
            if (rs.next()) {
                result = rs.getInt(1);
            }

        } finally {
            rs.close();
        }
        return result + 1;
    }

    /**
     * Возвращает имена и любой другой параметр столбца по номеру 1. TABLE_CAT
     * String => table catalog (may be null) 2.TABLE_SCHEM String => table
     * schema (may be null) 3.TABLE_NAME String => table name 4.COLUMN_NAME
     * String => column name 5.DATA_TYPE int => SQL type from java.sql.Types
     * 6.TYPE_NAME String => Data source dependent type name, for a UDT the type
     * name is fully qualified 7.COLUMN_SIZE int => column size. 8.
     * BUFFER_LENGTH is not used. 9.DECIMAL_DIGITS int => the number of
     * fractional digits. Null is returned for data types where DECIMAL_DIGITS
     * is not applicable. 10.NUM_PREC_RADIX int => Radix (typically either 10 or
     * 2) 11.NULLABLE int => is NULL allowed. columnNoNulls - might not allow
     * NULL values columnNullable - definitely allows NULL values
     * columnNullableUnknown - nullability unknown 12.REMARKS String => comment
     * describing column (may be null) 13.COLUMN_DEF String => default value for
     * the column, which should be interpreted as a string when the value is
     * enclosed in single quotes (may be null) 14.SQL_DATA_TYPE int => unused
     * 15.SQL_DATETIME_SUB int => unused 16.CHAR_OCTET_LENGTH int => for char
     * types the maximum number of bytes in the column 17.ORDINAL_POSITION int
     * => index of column in table (starting at 1) 18.IS_NULLABLE String => ISO
     * rules are used to determine the nullability for a column.
     * <p>
     * YES --- if the parameter can include NULLs NO --- if the parameter cannot
     * include NULLs empty string --- if the nullability for the parameter is
     * unknown
     * <p>
     * 19.SCOPE_CATLOG String => catalog of table that is the scope of a
     * reference attribute (null if DATA_TYPE isn't REF) 20.SCOPE_SCHEMA String
     * => schema of table that is the scope of a reference attribute (null if
     * the DATA_TYPE isn't REF) 21.SCOPE_TABLE String => table name that this
     * the scope of a reference attribure (null if the DATA_TYPE isn't REF)
     * 22.SOURCE_DATA_TYPE short => source type of a distinct type or
     * user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE
     * isn't DISTINCT or user-generated REF) 23.IS_AUTOINCREMENT String =>
     * Indicates whether this column is auto incremented YES --- if the column
     * is auto incremented NO --- if the column is not auto incremented empty
     * string --- if it cannot be determined whether the column is auto
     * incremented parameter is unknown
     *
     * @param nameTable -имя таблицы
     * @param nParam  -номера(номер) других параметров
     * @return имя=параметр
     */
    public static LinkedHashMap<String, Object> getMapNamesCol(Connection connection, String nameTable, Object nParam) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        Integer[] paramArr;

        if (nParam instanceof Integer[]) {

            paramArr = (Integer[]) nParam;
        } else {

            paramArr = new Integer[]{(Integer) nParam};
        }

        LinkedHashMap<String, Object> hmValues = new LinkedHashMap();

        DatabaseMetaData data;
        data = connection.getMetaData();

        nameTable = getNameTableByTypBase(connection, nameTable);
        ResultSet resultSet = data.getColumns(null, null, nameTable, null);

        try {
            while (resultSet.next()) {

                String nameCol = resultSet.getString(4).toLowerCase();

                Object[] parameters = new Object[paramArr.length];

                for (int i = 0; i < paramArr.length; i++) {
                    int iPar = paramArr[i];
                    Object Param = resultSet.getObject(iPar);
                    parameters[i] = Param;

                }

                if (paramArr.length > 1) {

                    hmValues.put(nameCol, parameters);
                } else {

                    hmValues.put(nameCol, parameters[0]);
                }
            }
        } finally {
            resultSet.close();
        }

        return hmValues;
    }

    /**
     * Возвращает имена и типы столбцов таблицы
     * @param nameTable -имя таблицы
     * @return имя=тип
     */
    public static LinkedHashMap<String, Integer> getNameAndTypeCol(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        LinkedHashMap<String, Integer> hmValues = new LinkedHashMap<String, Integer>();

        DatabaseMetaData data;
        data = connection.getMetaData();

        nameTable = getNameTableByTypBase(connection, nameTable);
        ResultSet resultSet = data.getColumns(null, null, nameTable, null);

        try {
            while (resultSet.next()) {

                String nameCol = resultSet.getString(4).toLowerCase();

                int typCol = resultSet.getInt(5);

                hmValues.put(nameCol, typCol);
            }
        } finally {
            resultSet.close();
        }

        return hmValues;
    }



    /**
     * Возвращает первичный ключ(ключи) таблицы в базе данных через ';'
     * @return Название первичного ключа
     */
    public static String getPrimaryKeyTable(Connection connection, String nameTable) throws SQLException {
        ResultSet rs = null;
        String name = null;

        StringBuilder builder = new StringBuilder();

        if (connection == null) {
            connection = connectCurrent;
        }

        nameTable = getNameTableByTypBase(connection, nameTable);
        DatabaseMetaData meta = connection.getMetaData();

        rs = meta.getPrimaryKeys(null, null, nameTable);

        try {
            while (rs.next()) {
                name = rs.getString(4).toLowerCase();

                builder.append(name);
                builder.append(";");

            }
        } finally {
            rs.close();
        }

        int lidx = builder.lastIndexOf(";");

        if (lidx != -1) {
            builder.deleteCharAt(lidx);
        }
        return builder.toString();
    }

    /**
     * Возвращает названия всех существующих таблиц и представлений в базе
     * данных
     *
     * @return HashMap названий таблиц=typ "TABLE", "VIEW" если bAll false то
     * только таблицы иначе все
     */
    public static HashMap<String, String> getNameAndTypeTables(Connection connection, boolean bAll) throws SQLException {
        HashMap<String, String> tables = null;
        ResultSet rs = null;

        if (connection == null) {
            connection = connectCurrent;
        }

        DatabaseMetaData meta = connection.getMetaData();
        tables = new HashMap<String, String>();

        if (bAll) {
            rs = meta.getTables(null, null, null, new String[]{"TABLE", "VIEW"});
        } else {
            rs = meta.getTables(null, null, null, new String[]{"TABLE"});

        }

        try {

            while (rs.next()) {
                String name = rs.getString(3).toLowerCase();
                String type = rs.getString(4).toLowerCase();

                tables.put(name, type);

            }
        } finally {
            rs.close();
        }
        return tables;
    }

    /**
     * Возвращает названия всех существующих таблиц и представлений в базе
     * данных
     * @return TreeSet названий таблиц
     */
    public static TreeSet<String> getNameTables(Connection connection) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        TreeSet<String> tables = null;
        ResultSet rs = null;
        DatabaseMetaData meta = connection.getMetaData();
        tables = new TreeSet<String>();
        rs = meta.getTables(null, null, null, new String[]{"TABLE", "VIEW"});

        try {

            while (rs.next()) {
                String name = rs.getString(3);
                tables.add(name.toLowerCase());

            }
        } finally {
            rs.close();
        }
        return tables;
    }

    /**
     *
     *
     * @param connection
     * @param sql
     * @return
     * @throws SQLException
     */
    public static ArrayList<String> getNamesTabBySQL(Connection connection, String sql) throws SQLException {

        ArrayList<String> result = new ArrayList<String>();

        TreeSet<String> tsNTables = SqlTask.getNamesDb(connection, false);

        for (String nt : tsNTables) {

            if (sql.contains(nt)) {

                result.add(nt);
            }
        }
        return result;
    }


    /**
     * Возвращает названия всех существующих таблиц и представлений в базе
     * данных
     *
     * @param connection
     * @param bView true - таблиц и представлений
     * @return TreeSet названий таблиц
     * @throws java.sql.SQLException
     */
    public static TreeSet<String> getNamesDb(Connection connection, boolean bView) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        TreeSet<String> tables;
        ResultSet rs;
        DatabaseMetaData meta = connection.getMetaData();

        tables = new TreeSet<String>();

        if (bView) {
            rs = meta.getTables(null, null, null, new String[]{"TABLE", "VIEW"});
        } else {
            rs = meta.getTables(null, null, null, new String[]{"TABLE"});

        }

        try {

            while (rs.next()) {
                String name = rs.getString(3);
                tables.add(name.toLowerCase());

            }
        } finally {
            rs.close();
        }
        return tables;
    }

    /**
     * Возвращает строковое представление значения в зависимости от его типа
     *
     * @param DeffValue
     * @return
     */
    public static String getStringValueByTyp(Object DeffValue) {

        String result = null;

        if (DeffValue instanceof Timestamp) {

            DateTime dateTime;
            Timestamp timestamp = (Timestamp) DeffValue;

            dateTime = new DateTime(timestamp);

            DateTimeFormatter dtf = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss");
            result = dateTime.toString(dtf);

        } else {
            result = DeffValue.toString();
        }

        return result;
    }

    public static Object getValueByTyp(String DeffValue, String typCol) throws NullPointerException {

        Object result = null;
        if (typCol.equalsIgnoreCase(Integer.class.getSimpleName())) {

            try {
                result = new Integer(DeffValue.trim());

            } catch (NumberFormatException nfe) {

                result = new Integer(0);
            }

        } else if (typCol.equalsIgnoreCase(Float.class.getSimpleName())) {

            try {
                result = new Float(DeffValue.trim());

            } catch (NumberFormatException nfe) {
                result = new Float(0);
            }

        } else if (typCol.equalsIgnoreCase(Double.class.getSimpleName())) {

            try {
                result = new Double(DeffValue);
            } catch (NumberFormatException nfe) {
                result = new Double(0);
            }

        } else if (typCol.equalsIgnoreCase(Boolean.class.getSimpleName())) {

            Boolean.getBoolean(DeffValue.trim());

        } else if (typCol.equalsIgnoreCase(Short.class.getSimpleName())) {

            try {
                result = new Short(DeffValue.trim());
            } catch (NumberFormatException nfe) {
                short s = 0;
                result = new Short(s);
            }
        } else if (typCol.equalsIgnoreCase(Byte.class.getSimpleName())) {

            try {
                result = new Byte(DeffValue.trim());
            } catch (NumberFormatException nfe) {
                byte s = 0;
                result = new Byte(s);
            }
        } else if (typCol.equalsIgnoreCase(Timestamp.class.getSimpleName())) {

            DateTime dateTime;
            Timestamp timestamp;

            try {

                DateTimeFormatter dtf;

                if (DeffValue == null || DeffValue.isEmpty()) {

                    dateTime = new DateTime();
                    timestamp = new Timestamp(dateTime.getMillis());

                } else {

                    if (DeffValue.indexOf("-") == -1) {

                        dtf = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss");

                    } else {

                        dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.S");

                    }

                    dateTime = dtf.parseDateTime(DeffValue.trim());
                    timestamp = new Timestamp(dateTime.getMillis());

                }
                result = timestamp;

            } catch (Exception e) {

                //   DbfClass.setLog("Проверте ввод даты !", e);
                dateTime = new DateTime();
                timestamp = new Timestamp(dateTime.getMillis());

                result = timestamp;

            }

        } else if (typCol.equalsIgnoreCase(String.class.getSimpleName())) {
            result = DeffValue.trim();

        } else {
            // Такого типа нет !

            throw new NullPointerException("Тип " + typCol + " в базе данных не предусмотрен!");

        }

        return result;
    }

    /**
     * Создается представление данных по Документу конфигурации
     */
    public static void createViewByConfig(Connection connection, NodeList list, String nameTable, String c_partype_id) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        // String c_partype_id = document.getDocumentElement().getAttribute("c_partype_id");
        ArrayList<String> alNames = new ArrayList<String>();

        String sql = "DROP VIEW " + nameTable;

        try {

            executeSql(connection, sql);
        } catch (SQLException e) {
        }

        StringBuilder builder = new StringBuilder("CREATE VIEW " + nameTable + " (");

        for (int i = 0; i < list.getLength(); i++) {

            // столбец
            Element e = (Element) list.item(i);

            String nameCol = e.getAttribute("name");
            alNames.add(nameCol);

            builder.append(nameCol);
            builder.append(",");

        }

        int idx = builder.lastIndexOf(",");
        builder.delete(idx, idx + 1);

        builder.append(") AS SELECT ");

        for (String s : alNames) {

            builder.append(s);
            builder.append(",");

        }
        idx = builder.lastIndexOf(",");
        builder.delete(idx, idx + 1);

        builder.append(" FROM values_current WHERE parnumber_id=");
        builder.append(c_partype_id);

        sql = builder.toString();

        executeSql(connection, sql);

    }

    /**
     * Создается новая таблица по Документу конфигурации
     */
    public static void createTableByConfig(Connection connection, NodeList list, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ArrayList<String> types = SqlTask.getTypInfo(connection);
        StringBuffer buffer = new StringBuffer("CREATE TABLE " + nameTable + " (");


        HashMap<String, ArrayList<String>> hmKeys = new HashMap<String, ArrayList<String>>();

        ArrayList<String> alTypes;
        alTypes = SqlTask.getTypInfo(null);

        boolean okTyp = false;

        for (int i = 0; i < list.getLength(); i++) {

            // столбец
            Element e = (Element) list.item(i);
            String nameCol = e.getAttribute("name");
            String typeCol = e.getAttribute("data_type");

            for (String styp : alTypes) {

                if (typeCol.contains(styp)) {
                    okTyp = true;
                    break;
                }
            }

            if (!okTyp) {
                throw new SQLException("тип '" + typeCol + "' не поддерживается!");
            }

            if (typeCol.isEmpty()) {
                continue;
            }

            String keyCol = e.getAttribute("key");
            String increment = "";

            if (keyCol.indexOf("INCREMENT") != -1) {
                // автоинкремент

                //    increment = bundle.getString("INCREMENT");
            }


            if (!keyCol.trim().isEmpty()) {

                if (hmKeys.containsKey(keyCol)) {

                    ArrayList<String> al = hmKeys.get(keyCol);

                    al.add(nameCol);

                } // новый
                else {
                    ArrayList<String> al = new ArrayList<String>();
                    al.add(nameCol);

                    hmKeys.put(keyCol, al);

                }
            }

            buffer.append(nameCol);
            buffer.append(" ");

            buffer.append(typeCol);
            buffer.append(" ");
            if (!keyCol.trim().isEmpty()) {
                buffer.append(keyCol);
            }

// авто инкремент
            buffer.append(increment);

            buffer.append(",");

        }

        if (buffer.lastIndexOf(",") == (buffer.length() - 1)) {

            buffer.deleteCharAt(buffer.lastIndexOf(","));
        }

        buffer.append(")");

        String sql = buffer.toString();
        SqlTask.executeSql(connection, sql);

        // Таблица создана
    }

    /**
     * Добавляем столбец по конфигурации
     *
     * @param nameCol
     */
    public static void addColByConfig(Connection connection, String nameTable, String nameCol, Element e) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        StringBuilder builder = new StringBuilder("ALTER TABLE ");
        builder.append(nameTable);

        String typeCol = e.getAttribute("type");
        String sizeCol = e.getAttribute("size");

        String sDriver;
        sDriver = connection.getMetaData().getDriverName();
        Locale locale_dbf = getLocaleByDriver(sDriver);
        ResourceBundle bundle = ResourceBundle.getBundle("DbfRes", locale_dbf);

        String sAdd = bundle.getString("addCol");
        typeCol = bundle.getString(typeCol);

        builder.append(" ");
        builder.append(sAdd);
        builder.append(" ");
        builder.append(nameCol);
        builder.append(" ");
        builder.append(typeCol);

        if (sizeCol != null && !sizeCol.isEmpty()) {

            builder.append("(");
            builder.append(sizeCol);
            builder.append(")");

        }

        String sql = builder.toString();
        executeSql(connection, sql);
    }

    /**
     * Удаляем столбец по конфигурации
     *
     * @param connection
     * @param nameTable
     * @param nameCol
     * @throws java.sql.SQLException
     */
    public static void delColByConfig(Connection connection, String nameTable, String nameCol) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        String sDriver;
        sDriver = connection.getMetaData().getDriverName();
        Locale locale_dbf = getLocaleByDriver(sDriver);
        ResourceBundle bundle = ResourceBundle.getBundle("DbfRes", locale_dbf);

        StringBuilder builder = new StringBuilder("ALTER TABLE ");
        builder.append(nameTable);

        String sDel = bundle.getString("dropCol");

        builder.append(" ");
        builder.append(sDel);
        builder.append(" ");
        builder.append(nameCol);
        builder.append(" ");

        String sql = builder.toString();
        executeSql(connection, sql);

    }

    public static Object getDeffValueByTypCol(Connection connection, String nameValue, String DeffValue, String nameTable) throws SQLException, NullPointerException {

        if (connection == null) {
            connection = connectCurrent;
        }

        Object result = null;

        HashMap<String, Object> hmTypes = getMapNamesCol(connection, nameTable, new Integer[]{5});

        Integer iTyp = (Integer) hmTypes.get(nameValue.toLowerCase());

        if (iTyp == Types.VARCHAR || iTyp == Types.CHAR) {

            result = DeffValue;

        } else if (iTyp == Types.BIT) {
            try {
                result = Byte.parseByte(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.INTEGER) {
            try {
                result = Integer.parseInt(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.SMALLINT) {
            try {
                result = Short.parseShort(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.TINYINT) {
            try {
                result = Byte.parseByte(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.BIGINT) {
            try {
                result = Long.parseLong(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.BOOLEAN) {
            try {
                result = Boolean.parseBoolean(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.DECIMAL) {
            try {
                result = Double.parseDouble(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.DOUBLE) {
            try {
                result = Double.parseDouble(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.FLOAT) {
            try {
                result = Float.parseFloat(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.NUMERIC) {
            try {
                result = Double.parseDouble(DeffValue);

            } catch (NumberFormatException nfe) {

                result = 0;
            }

        } else if (iTyp == Types.DATE || iTyp == Types.TIMESTAMP) {

            DateTimeFormatter dtf;
            Timestamp timestamp;
            DateTime dateTime;

            if (DeffValue == null || DeffValue.isEmpty()) {

                dateTime = new DateTime();
                timestamp = new Timestamp(dateTime.getMillis());

            } else {

                if (DeffValue.indexOf("-") == -1) {

                    dtf = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss");

                } else {

                    dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.S");

                }

                dateTime = dtf.parseDateTime(DeffValue.trim());
                timestamp = new Timestamp(dateTime.getMillis());

            }
            result = timestamp;

        } else {

            throw (new NullPointerException("Не распознанный формат данных !"));

        }

        return result;
    }

    public static Object getDeffValue(String nameValue, String DeffValue, Object nameTableORmap) throws SQLException, NullPointerException {
        Object result = null;
        HashMap<String, String> hmType = null;
        String nameTable;
        if (DeffValue == null) {
            return DeffValue;
        }

        if (nameTableORmap == null) {
            return DeffValue;

        }

        if (nameTableORmap instanceof String) {

            nameTable = (String) nameTableORmap;

            if (nameTable == null || nameTable.isEmpty()) {
                return DeffValue;

            }

            //   hmType = (HashMap<String, String>)getParamTableByName(nameTable, TABLE_TYPE_COL);
        }

        String typCol = null;

        /**
         * // для новой базы if (nameTableORmap instanceof Map) {
         *
         * hmType = (HashMap<String, String>) nameTableORmap;
         *
         * }
         *
         * String typCol = null; if (hmType.containsKey(nameValue)) { typCol =
         * hmType.get(nameValue); } else {
         *
         * throw new NullPointerException("Столбца " + nameValue + "xml
         * Конфигурации нет !"); }
         *
         */
        result = getValueByTyp(DeffValue, typCol);
        return result;
    }

    /**
     * Возврат ResultSet по сохраненному sql Запросу
     *
     * @param saveSql
     * @return
     */
    public static ResultSet getResultSetBySaveSql(Connection connection, String saveSql, int concur) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet rs = null;

        String[] sSql = saveSql.split("//");

        String sssql;
        String sssval;
        String sssnametab;
        String nameTable;

        if (sSql.length == 3) {

            sssql = sSql[0];
            sssval = sSql[1];
            sssnametab = sSql[2];

            ArrayList<Object> alValues = new ArrayList<Object>();

            nameTable = getNameTableByView(sssnametab);

            String sView = getViewByTable(nameTable);

            if (!sssnametab.equals(sView)) {

                sssql = sssql.replace(sssnametab, sView);

            }

            String values[] = sssval.split("##");

            for (String param : values) {
                String[] pv = param.split("=");
                String p = pv[0];
                String v = pv[1];

                Object object = null;

                object = getDeffValueByTypCol(null, p.trim(), v.trim(), nameTable);

                alValues.add(object);

            }

            rs = getResultSet(null, sssql, alValues, null, concur);

        } else {

            sssql = sSql[0];
            rs = getResultSet(connection, sssql, null, concur);
        }

        return rs;
    }

    public static String getStringSql(String sql, Object[] values) {

        String saveSql = sql;

        String s;

        for (Object o : values) {

            if (o instanceof String) {

                s = "'" + o + "'";

                saveSql = saveSql.replaceFirst("#", s);

            } else if (o instanceof Number) {

                s = o.toString();

                saveSql = saveSql.replaceFirst("#", s);

            } else if (o instanceof Timestamp) {

                s = "'" + o.toString() + "'";

                saveSql = saveSql.replaceFirst("#", s);

            } else {

                s = "'" + o.toString() + "'";

                saveSql = saveSql.replaceFirst("#", s);

            }

        }
        return saveSql;
    }

    public static String getSaveSql(String sql, List<String> names, List<Object> values, String nameTable) {

        String saveSql = sql;

        String s;

        for (Object o : values) {

            if (o instanceof String) {

                s = "'" + o + "'";

                saveSql = saveSql.replaceFirst("#", s);

            } else if (o instanceof Number) {

                s = o.toString();

                saveSql = saveSql.replaceFirst("#", s);

            } else if (o instanceof Timestamp) {

                s = "'" + o.toString() + "'";

                saveSql = saveSql.replaceFirst("#", s);

            } else {

                s = "'" + o.toString() + "'";

                saveSql = saveSql.replaceFirst("#", s);

            }

        }
        return saveSql;
    }

    /**
     * Название таблицы и код хранимого параметра по представлению
     *
     * @param nameView -имя представления
     * @return
     */
    public static Object[] getNameTabAndKodByView(Connection connection, String nameView) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        Object[] objects = new Object[2];

        Integer iKod = null;
        String nameTable;
        ResultSet rs = null;
        String sql = "SELECT * FROM " + nameView;

        rs = getResultSet(connection, sql, 1, ResultSet.CONCUR_READ_ONLY);

        nameTable = rs.getMetaData().getTableName(1).toLowerCase();

        try {

            if (rs.next() && !nameTable.equals(nameView)) {
                iKod = rs.getInt("parnumber_id");

            }

        } finally {
            rs.close();
        }

        objects[0] = nameTable;
        objects[1] = iKod;

        return objects;
    }

    /**
     * Количество строк в таблице
     * @param nameTable имя таблицы
     * @return
     */
    public static int getRowCount(String nameTable) {
        int result = 0;

        try {
            ResultSet resultSet = getResultSet(null, "SELECT COUNT(*) FROM " + nameTable);

            if (resultSet.next()) {

                result = resultSet.getInt(1);
            }

        } catch (SQLException ex) {
            result = -1;
        }

        return result;

    }

    /**
     * Название таблицы по представлению
     *
     * @param nameView -имя представления
     * @return
     */
    public static String getNameTableByView(String nameView) {

        String nameTable = nameView;

        if (HM_VIEWS.containsKey(nameView)) {

            return nameTable;
        } else {
            for (String name : HM_VIEWS.keySet()) {

                String nv = HM_VIEWS.get(name);

                if (nameView.equals(nv)) {

                    nameTable = name;

                    break;
                }
            }
        }
        return nameTable;
    }

    public static LinkedHashMap<String, Object> getMapValues(Connection connection, String sql, List values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }
        LinkedHashMap<String, Object> result;
        result = new LinkedHashMap<>();
        ResultSet rs;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                addParamToMap(rs, result);

            }

        } finally {
            rs.close();
        }
        return result;

    }

    public static LinkedHashMap<String, Object> getRowValues(Connection connection, String sql, Object[] values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }
        LinkedHashMap<String, Object> result;
        result = new LinkedHashMap<>();
        ResultSet rs;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                addParamToMap(rs, result);

            }

        } finally {
            rs.close();
        }
        return result;

    }

    /**
     * Карту в виде двух полей запроса к базе данных
     *
     * @param connection
     * @param sql-       sql запрос (Должно быть только два поля в запросе)
     * @param values-    значения запроса
     * @return
     * @throws java.sql.SQLException
     */
    public static LinkedHashMap<Object, Object> getMapBySQL(Connection connection, String sql, Object[] values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }
        LinkedHashMap<Object, Object> result;
        result = new LinkedHashMap();
        ResultSet rs;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                Object o1 = rs.getObject(1);
                Object o2 = rs.getObject(2);
                result.put(o1, o2);
            }

        } finally {
            rs.close();
        }
        return result;
    }

    /**
     * Карту в виде двух полей запроса к базе данных
     *
     * @param connection
     * @param sql-       sql запрос (Должно быть только два поля в запросе)
     * @param values-    значения запроса
     * @return
     * @throws java.sql.SQLException
     */
    public static LinkedHashMap<Object, Object> getMapBySQL(Connection connection, String sql, List values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }
        LinkedHashMap<Object, Object> result;
        result = new LinkedHashMap<>();
        ResultSet rs;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                Object o1 = rs.getObject(1);
                Object o2 = rs.getObject(2);
                result.put(o1, o2);
            }

        } finally {
            rs.close();
        }
        return result;
    }

    /**
     * Массив в виде всех полей
     *
     * @param sql-    sql запрос
     * @param values- значения запроса
     * @return
     */
    public static ArrayList<HashMap<String, Object>> getArrayMapBySQL(Connection connection, String sql, List values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ArrayList<HashMap<String, Object>> alMap = new ArrayList<>();

        ResultSet rs = null;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                HashMap<String, Object> hmres = new HashMap<>();

                addParamToMap(rs, hmres);
                alMap.add(hmres);
            }

        } finally {
            rs.close();
        }
        return alMap;
    }


    /**
     * Карту значений Объекта за период
     *
     * @param nameTable название тавлицы
     * @param idObject  id объекта
     * @param dateFirst начало периода
     * @param dateLast  конец периода
     * @return
     */
    public static HashMap<Timestamp, HashMap<String, Object>> getObjectValues(Connection connection, String nameTable,
                                                                              Integer idObject, DateTime dateFirst, DateTime dateLast) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }


        ResultSet rs = null;

        String names = "value_date, tarif";

        if (nameTable.equals("enegry_data")) {
            names = "value_date, energy_dowh_0_0,energy_dowh_0_1," +
                    "energy_dowh_0_2,energy_dowh_0_3,tarif";
        } else if (nameTable.equals("profil_power")) {

            names = "value_date, power_pa,power_pr,power_qa,power_qr";

        } else if (nameTable.equals("profil_current")) {

            names = "value_date, current_0,current_1,current_2";

        } else if (nameTable.equals("profil_voltage")) {

            names = "value_date, voltage_0,voltage_1,voltage_2";

        }

        String sql = "SELECT " + names + " FROM " + nameTable + "  WHERE  Id_object=? AND  value_date>= ? AND value_date<= ?";


        Timestamp tsFirst = new Timestamp(dateFirst.getMillis());

        Timestamp tsLast = new Timestamp(dateLast.getMillis());

        rs = getResultSet(connection, sql, new Object[]{idObject, tsFirst, tsLast});


        HashMap<Timestamp, HashMap<String, Object>> result = new HashMap<>();


        try {
            while (rs.next()) {

                HashMap<String, Object> hmres = new HashMap<>();

                addParamToMap(rs, hmres);

                Timestamp timestamp = (Timestamp) hmres.get("value_date");
                hmres.remove("value_date");
                result.put(timestamp, hmres);

            }

        } finally {
            rs.close();
        }
        return result;
    }


    /**
     * Массив в виде всех полей
     *
     * @param sql-    sql запрос
     * @param values- значения запроса
     * @return
     */
    public static ArrayList<HashMap<String, Object>> getArrayMapBySQL(Connection connection, String sql, Object[] values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ArrayList<HashMap<String, Object>> alMap = new ArrayList<>();

        ResultSet rs = null;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                HashMap<String, Object> hmres = new HashMap<>();

                addParamToMap(rs, hmres);
                alMap.add(hmres);
            }

        } finally {
            rs.close();
        }
        return alMap;
    }

    /**
     * Список из запрошенных данных
     *
     * @param sql-    sql запрос (Должно быть только одно поле в запросе)
     * @param values- значения запроса
     * @return
     */
    public static ArrayList<Object> getListBySQL(Connection connection, String sql, Object[] values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }
        ArrayList<Object> result = new ArrayList<>();

        ResultSet rs = null;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                Object o1 = rs.getObject(1);
                result.add(o1);
            }

        } finally {
            rs.close();
        }
        return result;
    }

    /**
     * Список из запрошенных данных
     *
     * @param sql-    sql запрос (Должно быть только одно поле в запросе)
     * @param values- значения запроса
     * @return
     */
    public static ArrayList<Object> getListBySQL(Connection connection, String sql, List values) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }
        ArrayList<Object> result = new ArrayList<>();

        ResultSet rs = null;

        if (values != null) {
            rs = getResultSet(connection, sql, values);
        } else {

            rs = getResultSet(connection, sql);

        }
        try {
            while (rs.next()) {

                Object o1 = rs.getObject(1);
                result.add(o1);
            }

        } finally {
            rs.close();
        }
        return result;
    }

    public static ResultSet getResultSet(Connection connection, String sql, ArrayList<Object> alValues, Integer countRow) throws SQLException {

        ResultSet rs = null;

        if (connection == null) {
            connection = connectCurrent;
        }

        PreparedStatement statement = connection.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        int i = 1;
        for (Object object : alValues) {

            statement.setObject(i, object);
            i++;
        }

        statement.setFetchSize(1);

        if (countRow != null) {

            statement.setMaxRows(countRow);

        }

        rs = statement.executeQuery();

        return rs;

    }

    public static ResultSet getResultSet(Connection connection, String sql, ArrayList<Object> alValues, Integer countRow, int concur) throws SQLException {

        ResultSet rs = null;

        if (connection == null) {
            connection = connectCurrent;
        }

        PreparedStatement statement = connection.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE, concur);

        int i = 1;
        for (Object object : alValues) {

            statement.setObject(i, object);
            i++;
        }

        statement.setFetchSize(1);

        if (countRow != null) {

            statement.setMaxRows(countRow);

        }

        rs = statement.executeQuery();

        return rs;

    }

    public static ResultSet getResultSet(Connection connection, String sql, Object[] Values, Integer countRow, int concur) throws SQLException {

        ResultSet rs = null;

        if (connection == null) {
            connection = connectCurrent;
        }

        PreparedStatement statement = connection.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE, concur);

        int i = 1;
        for (Object object : Values) {

            statement.setObject(i, object);
            i++;
        }

        statement.setFetchSize(1);

        if (countRow != null) {

            statement.setMaxRows(countRow);

        }

        rs = statement.executeQuery();

        return rs;

    }

    /**
     * Возвращает имя столбца -первичного ключа по названию таблицы о
     */
    public static HashMap<String, Object> getPrimaryKeyMap(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        nameTable = getNameTableByTypBase(connection, nameTable);
        HashMap<String, Object> result = new HashMap<String, Object>();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, null, nameTable);
        try {
            while (resultSet.next()) {

                String key = resultSet.getString(4).toLowerCase();

                result.put(key, null);
            }

        } finally {
            resultSet.close();
        }
        return result;

    }

    /**
     * Возвращает имя столбца -первичного ключа по названию таблицы о
     */
    public static TreeMap<String, Object> getMapPrimaryKey(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        nameTable = getNameTableByTypBase(connection, nameTable);

        TreeMap<String, Object> result = new TreeMap<>();
        DatabaseMetaData databaseMetaData = connection.getMetaData();

        ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, null, nameTable);
        try {
            while (resultSet.next()) {

                String key = resultSet.getString(4).toLowerCase();

                result.put(key, null);
            }

        } finally {
            resultSet.close();
        }
        return result;

    }

    /**
     * Возвращает имя таблицы как в базе с учетом регистра
     *
     * @param connection
     * @param nameTable
     * @return
     * @throws SQLException
     */
    public static String getNameTableByTypBase(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {

            connection = connectCurrent;
        }
        String result = nameTable;
        ResultSet rs = null;
        DatabaseMetaData meta = connection.getMetaData();

        rs = meta.getTables(null, null, null, new String[]{"TABLE", "VIEW"});
        try {

            while (rs.next()) {
                String name = rs.getString(3);

                if (name.equalsIgnoreCase(nameTable)) {
                    result = name;
                    break;
                }

            }
        } finally {
            rs.close();
        }

        return result;
    }

    /**
     * Возвращает имя столбца -первичного ключа по названию таблицы о
     */
    public static ArrayList<String> getPrimaryKey(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        nameTable = getNameTableByTypBase(connection, nameTable);
        ArrayList<String> result = new ArrayList<String>();
        DatabaseMetaData databaseMetaData = connection.getMetaData();

        ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, null, nameTable);
        try {
            while (resultSet.next()) {

                String key = resultSet.getString(4).toLowerCase();

                result.add(key);
            }

        } finally {
            resultSet.close();
        }
        return result;

    }

    /**
     * C прокруткой , но без учета изменений в базе данных
     *
     * @param sql
     * @param countRow -Количество возвращаемы строк, если null то все
     * @return
     */
    public static ResultSet getResultSet(Connection connection, String sql, Integer countRow, int concur) throws SQLException {

        Statement stat = null;

        if (connection == null) {
            connection = connectCurrent;
        }

        stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                concur);

        if (countRow != null) {

            stat.setMaxRows(countRow);
        }

        stat.setFetchSize(1);
        return stat.executeQuery(sql);

    }

    /**
     * Возврат индексов таблицы
     *
     * @param nameTable
     * @return
     */
    public static ArrayList<String> getArrayIndexTable(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        nameTable = getNameTableByTypBase(connection, nameTable);
        ArrayList<String> result = new ArrayList<String>();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet resultSet = databaseMetaData.getIndexInfo(null, null, nameTable, false, false);

        try {

            while (resultSet.next()) {

                String nameIndex = resultSet.getString(9).toLowerCase();

                if (nameIndex != null) {
                    result.add(nameIndex);

                }

            }

        } finally {
            resultSet.close();
        }

        return result;

    }

    /**
     * Название и тип столбца в строковом представлении
     *
     * @param connection
     * @param nameTable
     * @return
     * @throws SQLException
     */
    public static HashMap<String, String> getTypColumns(Connection connection, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        nameTable = getNameTableByTypBase(connection, nameTable);

        HashMap<String, String> result = new HashMap<String, String>();

        DatabaseMetaData data;
        data = connection.getMetaData();

        ResultSet resultSet = data.getColumns(null, null, nameTable, null);
        try {
            while (resultSet.next()) {

                String nameCol = resultSet.getString(4).toLowerCase();

                String typCol = resultSet.getString(6);

                result.put(nameCol, typCol);
            }
        } finally {
            resultSet.close();
        }

        return result;
    }

    /**
     * Вставляет новую запись в таблицу возвращает номер вставленой записи если
     * -1 то проблемы с вставкой файла
     *
     * @param nameTable
     * @param hmCol     карта записываемых столбцов в виде имя-значение
     */
    public static Integer insertRecInTable(Connection connection, String nameTable, HashMap<String, Object> hmCol) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        nameTable = getNameTableByTypBase(connection, nameTable);

        Integer id_key = null;

        PreparedStatement ps = null;

        StringBuilder sbName = new StringBuilder();
        StringBuilder sbValue = new StringBuilder();

        if (hmCol == null || hmCol.isEmpty()) {

            return null;
        }

        String sql = "";

        sbName.append("INSERT INTO ");

        sbName.append(nameTable);
        sbName.append(" (");
        sbValue.append(" VALUES (");

        for (String ncol : hmCol.keySet()) {
            sbName.append(ncol);
            sbName.append(",");

            sbValue.append("?,");

        }

        sbName.deleteCharAt(sbName.lastIndexOf(","));
        sbName.append(")");
        sbValue.deleteCharAt(sbValue.lastIndexOf(","));
        sbValue.append(")");

        sql = sbName.toString() + sbValue.toString();

        ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        try {

            int i = 1;

            for (String ncol : hmCol.keySet()) {
                Object val = hmCol.get(ncol);

                if (val instanceof File) {
                    try {
                        File file = (File) val;

                        FileInputStream fis = new FileInputStream(file);
                        ps.setBinaryStream(i, fis, file.length());
                        try {
                            fis.close();
                        } catch (IOException ex) {

                            throw new SQLException(ex.getMessage());
                        }

                    } catch (FileNotFoundException ex) {

                        throw new SQLException(ex.getMessage());

                    }

                } else {
                    ps.setObject(i, val);
                }

                i++;
            }

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs != null) {

                try {
                    if (rs.next()) {
                        id_key = rs.getInt(1);
                    }

                } finally {
                    rs.close();
                }
            }

        } finally {
            ps.close();
        }

        return id_key;
    }

    public static ResultSet getResultSet(Connection connection, Map<String, Object> hmKeys, String nameTable) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        StringBuilder builder = new StringBuilder();
        ResultSet resultSet = null;
        Statement stat = null;

        String sql;

        builder.append("SELECT * FROM ");
        builder.append(nameTable);
        builder.append(" WHERE ");

        for (String key : hmKeys.keySet()) {

            builder.append(key);
            builder.append("=? AND ");
        }

        int idx = builder.lastIndexOf("AND");

        builder.delete(idx, idx + 4);
        sql = builder.toString();
        Object[] objects = hmKeys.values().toArray();
        resultSet = getResultSet(connection, sql, objects);

        return resultSet;

    }

    /**
     * Возвращает тип таблицы по названию таблицы
     *
     * @param nameTable название таблицы
     * @return тип таблицы
     */
    public static int getTypTableByNameTable(Connection connection, String nameTable) throws SQLException {

        int result = 0;

        String sql;

        if (connection == null) {
            connection = connectCurrent;
        }

        sql = "SELECT c_obj_type FROM c_obj_spec WHERE c_name_table=?";
        ResultSet rs = getResultSet(connection, sql, new Object[]{nameTable});

        try {

            if (rs.next()) {

                result = rs.getInt("c_obj_type");
            }

        } finally {
            rs.close();
        }
        return result;
    }

    /**
     * Возвращает имя и название таблиц
     *
     * @param typTable тип таблицы если Null то по всем
     * @return карту имя=название
     */
    public static HashMap<String, String> getNameAndCaptionTablesByTyp(Connection connection, Integer typTable) throws SQLException {

        String sql;

        if (connection == null) {
            connection = connectCurrent;
        }

        if (typTable != null) {
            sql = "SELECT c_obj_list_name, c_name_table FROM c_obj_spec WHERE c_obj_type=" + typTable;
        } else {

            sql = "SELECT  c_obj_list_name, c_name_table FROM c_obj_spec";

        }
        HashMap<String, String> result = new HashMap<String, String>();
        ResultSet resultSet = SqlTask.getResultSet(null, sql);

        try {

            while (resultSet.next()) {

                String capt = resultSet.getString("c_obj_list_name");
                String name = resultSet.getString("c_name_table");

                result.put(name, capt);
            }

        } finally {
            resultSet.close();
        }

        return result;
    }

    public static ResultSet getResultSetLimit(Connection connection, String sql, int maxRow, int concur) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet resultSet = null;
        Statement stat = null;

        stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                concur);

        stat.setFetchSize(1);
        stat.setMaxRows(maxRow);

        resultSet = stat.executeQuery(sql);

        return resultSet;

    }

    public static ResultSet getResultSet(Connection connection, String sql, int concur) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet resultSet = null;
        Statement stat = null;

        stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                concur);

        stat.setFetchSize(1);
        resultSet = stat.executeQuery(sql);

        return resultSet;

    }

    public static ResultSet getResultSet(Connection connection, String sql) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet resultSet = null;
        Statement stat = null;

        stat = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        stat.setFetchSize(1);
        resultSet = stat.executeQuery(sql);

        return resultSet;

    }

    public static boolean executeSql(Connection connection, String sql) throws SQLException {

        Statement stat = null;

        if (connection == null) {
            connection = connectCurrent;
        }

        stat = connection.createStatement();
        return stat.execute(sql);
    }

    public static boolean executeSql(Connection connection, String sql, Object[] objects) throws SQLException {

        boolean result = false;

        if (connection == null) {
            connection = connectCurrent;
        }

        PreparedStatement ps = connection.prepareStatement(sql);

        try {
            int i = 1;
            for (Object object : objects) {

                ps.setObject(i, object);
                i++;
            }

            result = ps.execute();

        } finally {
            ps.close();
        }
        return result;

    }

    public static int executeUpdateSQL(Connection connection, String sql, Object[] objects, Integer maxRow) throws SQLException {

        int result = 0;

        if (connection == null) {
            connection = connectCurrent;
        }

        PreparedStatement ps = connection.prepareStatement(sql);

        try {
            if (maxRow != null) {
                ps.setMaxRows(maxRow);
            }
            int i = 1;
            for (Object object : objects) {

                ps.setObject(i, object);
                i++;
            }

            result = ps.executeUpdate();

        } finally {
            ps.close();
        }
        return result;

    }

    public static int executeUpdateSQL(Connection connection, String sql, List objects, Integer maxRow) throws SQLException {

        int result = 0;

        if (connection == null) {
            connection = connectCurrent;
        }

        PreparedStatement ps = connection.prepareStatement(sql);

        try {
            if (maxRow != null) {
                ps.setMaxRows(maxRow);
            }
            int i = 1;
            for (Object object : objects) {

                ps.setObject(i, object);
                i++;
            }

            result = ps.executeUpdate();

        } finally {
            ps.close();
        }
        return result;

    }

    public static ResultSet getResultSet(Connection connection, String sql, Object[] objects, PreparedStatement statement) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        PreparedStatement ps;

        ResultSet resultSet = null;

        if (statement == null) {
            ps = connection.prepareStatement(sql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
        } else {
            ps = statement;
        }

        int i = 1;
        for (Object object : objects) {

            ps.setObject(i, object);
            i++;
        }

        resultSet = ps.executeQuery();
        return resultSet;

    }

    public static Object[] getResultSetAndStringSql(Connection connection, String sql, Object[] objects) throws SQLException {

        Object[] os = new Object[2];

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet resultSet = null;

        PreparedStatement ps = connection.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        int i = 1;
        for (Object object : objects) {

            ps.setObject(i, object);
            i++;
        }

        String sqlString = ps.toString();
        sqlString = sqlString.substring(sqlString.indexOf("SELECT"), sqlString.length());
        resultSet = ps.executeQuery();

        os[0] = resultSet;
        os[1] = sqlString;
        return os;
    }

    public static ResultSet getResultSet(Connection connection, String sql, Object[] objects) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet resultSet = null;

        PreparedStatement ps = connection.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        int i = 1;
        for (Object object : objects) {

            ps.setObject(i, object);
            i++;
        }
        resultSet = ps.executeQuery();
        return resultSet;

    }

    public static ResultSet getResultSet(Connection connection, String sql, List objects) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet resultSet = null;

        PreparedStatement ps = connection.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        int i = 1;
        for (Object object : objects) {

            ps.setObject(i, object);
            i++;
        }
        resultSet = ps.executeQuery();
        return resultSet;

    }

    public static ResultSet getResultSet(Connection connection, String sql, Object[] objects, Integer countRow) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        ResultSet resultSet = null;

        PreparedStatement ps = connection.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);

        ps.setMaxRows(countRow);
        ps.setFetchSize(1);

        int i = 1;
        for (Object object : objects) {

            ps.setObject(i, object);
            i++;
        }

        resultSet = ps.executeQuery();
        return resultSet;

    }

    public static Locale getCurrentLocale() throws NullPointerException {

        String driver = null;

        Locale locale_dbf = null; // Текущая локализация
        if (driver.equals("com.microsoft.sqlserver.jdbc.SQLServerDriver")) {
            locale_dbf = new Locale("ru", "DB", "mssql");
        } else if (driver.equals("com.mysql.jdbc.Driver")) {
            locale_dbf = new Locale("ru", "DB", "mysql");
        } else if (driver.equals("org.hsqldb.jdbcDriver")) {
            locale_dbf = new Locale("ru", "DB", "hsqldb");
        } else if (driver.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
            locale_dbf = new Locale("ru", "DB", "derby");
        } else {
            LOGGER.error("Нет драйвера DB !");
            throw new NullPointerException("Нет драйвера DB !");
        }
        return locale_dbf;

    }

    public static Locale getLocaleByDriver(String driver) throws NullPointerException {

        Locale locale_dbf = null; // Текущая локализация
        if (driver.toLowerCase().contains("sqlserver")) {
            locale_dbf = new Locale("ru", "DB", "mssql");
        } else if (driver.toLowerCase().contains("mysql")) {
            locale_dbf = new Locale("ru", "DB", "mysql");
        } else if (driver.equals("org.hsqldb.jdbcDriver")) {
            locale_dbf = new Locale("ru", "DB", "hsqldb");
        } else if (driver.toLowerCase().contains("derby")) {
            locale_dbf = new Locale("ru", "DB", "derby");
        } else {
            LOGGER.error("Нет драйвера DB !");
            throw new NullPointerException("Нет драйвера DB !");
        }

        localeCurrent = locale_dbf;
        return locale_dbf;
    }

    /**
     *  Возвращает тип базы
     * @param connection-текущее присоединение
     * @return  0-MySQL 1-Derby
     * @throws SQLException
     */

    public static int getTypeBase(Connection connection) throws SQLException {

        int result = 0;

        if (connection == null) {
            connection = connectCurrent;
        }


        String nameBase = connection.getMetaData().getDatabaseProductName();

        if (nameBase.equals("MySQL")) {

            result = 0;

        } else {
            result = 1;
        }


        return result;
    }


    public static boolean isSxemaSet(Connection connection, String nameSxem) throws SQLException {

        if (connection == null) {
            connection = connectCurrent;
        }

        boolean result = false;

        ResultSet rsSxems = connection.getMetaData().getSchemas();

        try {

            while (rsSxems.next()) {

                String name = rsSxems.getString(1);

                if (name.equalsIgnoreCase(nameSxem)) {
                    result = true;
                    break;
                }
            }
        } finally {
            rsSxems.close();
        }
        return result;
    }


    public static Connection openTestBase(String path,String name) throws Exception {

        Connection connection = null;

        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String user = "pes";
        String password = "silesta";

        Class.forName(driver).newInstance(); //загрузка драйвера, который должен быть в CLASSPATH

        File file = new File(path, name);

        String uri_con = file.getAbsolutePath();

        uri_con = "jdbc:derby:" + uri_con;


        Properties properties = new Properties();

        properties.put("user", user);
        properties.put("password", password);

        connection = DriverManager.getConnection(uri_con, properties);


        return connection;

    }


    public static Connection openBase(String driver, String url, String port, String basename, String dopInfo, String username, String password) throws Exception {

        Connection connection = null;

        File file = null;

        Locale locale_dbf = getLocaleByDriver(driver); // Текущая локализация

        Class.forName(driver).newInstance(); //загрузка драйвера, который должен быть в CLASSPATH

        ResourceBundle bundle = ResourceBundle.getBundle("DbfRes", locale_dbf);
        String jdbc = bundle.getString(CONN_JDBC);
        // String nameBaseURL = bundle.getString(CONN_NAME_BASE);
        String newBase = bundle.getString(CONN_NEW_BASE);
        StringBuilder sb = new StringBuilder(jdbc);
        if (driver.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
            // локальная база

            if (dopInfo.endsWith("DeffBase")) {
                dopInfo = System.getProperty("user.dir");
                file = new File(dopInfo, "DeffBase");

            }

            if (file == null) {
                file = new File(dopInfo);
            }

            sb.append(file.getAbsolutePath());
            // если не найдена то новая даза
            if (!file.exists()) {
                int isel = createNewBaseDialog(basename, null);
                if (isel == 0) {
                    sb.append(newBase);
                } else {
                    return null;
                }
            }
        } else {
            // серверная база
            sb.append(url);
            sb.append(":");
            sb.append(port);
        }
        String uri_con = sb.toString();
        if (uri_con.indexOf("localhost") != -1) {
        }
        if (basename == null || basename.isEmpty()) {
            return null;
        }
        Properties properties = new Properties();

        properties.put("user", username);
        properties.put("password", password);
        // properties.put("useOldAliasMetadataBehavior", "false");

        connection = DriverManager.getConnection(uri_con, properties);

        try {
            connection.setCatalog(basename);
        } catch (SQLException ex) {

            int isel = createNewBaseDialog(basename, null);

            if (isel == 0) {
                String sql = "CREATE DATABASE " + basename;
                SqlTask.executeSql(connection, sql);
                connection.setCatalog(basename);

            } else {

                return null;
            }
        }

        return connection;

    }
}
