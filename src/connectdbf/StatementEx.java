package connectdbf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author 1
 */
public class StatementEx {

    public Connection connection;
    private PreparedStatement psInsert = null;
    private PreparedStatement psReplace = null;
    private PreparedStatement psQuery = null;
    private boolean GenerateKey;
    private TreeMap<String, Object> mapKeys;

    private String nameTable;
    private HashMap<String, Object> hmCol;
    private boolean isBatch;// Пакетное обновление

    /**
     * Для запроса данных
     *
     * @param connection
     * @param sql        -строка запроса
     * @throws SQLException
     */
    public StatementEx(Connection connection, String sql) throws SQLException {
        this.GenerateKey = false;

        if (connection == null) {
            this.connection = SqlTask.connectCurrent;
        } else {
            this.connection = connection;
        }

        this.nameTable = sql;
        createQueryStmt();

    }

    /**
     * Для организации вставки или замены данных
     *
     * @param connection
     * @param nameTable
     * @param hmCol      - если null то учавствуют Все столбцы таблицы
     * @throws SQLException
     */
    public StatementEx(Connection connection, String nameTable, HashMap<String, Object> hmCol) throws SQLException {
        this.GenerateKey = false;

        if (connection == null) {
            this.connection = SqlTask.connectCurrent;
        } else {
            this.connection = connection;
        }

        if (hmCol != null) {

            if (hmCol.containsKey("name_table")) {

                hmCol.remove("name_table");
            }


        }


        this.hmCol = hmCol;
        this.nameTable = nameTable;

        // isBatch=connection.getMetaData().supportsBatchUpdates();
        createReplaceStmt();
        createInsertStmt();

    }


    /**
     * Для организации вставки данных
     *
     * @param connection
     * @param nameTable
     * @param hmCol-      если null то учавствуют Все столбцы таблицы
     * @param GenerateKey если true то возвращает значение первичного ключа
     * @throws SQLException
     */
    public StatementEx(Connection connection, String nameTable, HashMap<String, Object> hmCol, boolean GenerateKey) throws SQLException {

        this.GenerateKey = GenerateKey;

        if (connection == null) {
            this.connection = SqlTask.connectCurrent;
        } else {
            this.connection = connection;
        }
        this.hmCol = hmCol;
        this.nameTable = nameTable;

        createInsertStmt();

    }

    public HashMap<String, Object> getHmCol() {
        return hmCol;
    }

    public void close() throws SQLException {

        if (psInsert != null) {
            psInsert.close();
        }

        if (psReplace != null) {
            psReplace.close();
        }

        if (psQuery != null) {
            psQuery.close();
        }

    }

    private void createQueryStmt() throws SQLException {

        psQuery = connection.prepareStatement(nameTable);

    }

    public ResultSet getResultSet(Object[] parameters) throws SQLException {

        ResultSet resultSet = null;

        for (int i = 1; i <= parameters.length; i++) {
            psQuery.setObject(i, parameters[i - 1]);
        }

        resultSet = psQuery.executeQuery();

        return resultSet;
    }

    private void createReplaceStmt() throws SQLException {

        StringBuilder sbValues = new StringBuilder();

        String sql = "";

        if (hmCol == null || hmCol.isEmpty()) {

            hmCol = SqlTask.getMapNamesCol(connection, nameTable, 1);
        }

        mapKeys = SqlTask.getMapPrimaryKey(connection, nameTable);

        if (mapKeys.isEmpty()) {

            throw new NullPointerException(nameTable + "-Нет главного ключа!");
        }

        for (String s : mapKeys.keySet()) {

            if (!hmCol.containsKey(s)) {

                throw new NullPointerException(nameTable + "- нет ключа " + s);
            }

        }

        StringBuilder sbWhere = new StringBuilder();

        sbWhere.append(" WHERE ");

        for (String k : mapKeys.keySet()) {
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

            if (!mapKeys.containsKey(ncol)) {
                sbValues.append(ncol);
                sbValues.append("=?,");
            }
        }
        sbValues.deleteCharAt(sbValues.lastIndexOf(","));

        sbValues.append(" ");
        sbValues.append(sbWhere.toString());

        sql = sbValues.toString();

        if (GenerateKey) {
            psReplace = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        } else {

            psReplace = connection.prepareStatement(sql);
        }

    }

    private void createInsertStmt() throws SQLException {

        StringBuilder sbName = new StringBuilder();
        StringBuilder sbValue = new StringBuilder();

        if (hmCol == null || hmCol.isEmpty()) {

            hmCol = SqlTask.getMapNamesCol(connection, nameTable, 1);
        }

        mapKeys = SqlTask.getMapPrimaryKey(connection, nameTable);

        if (mapKeys.isEmpty()) {

            throw new NullPointerException(nameTable + "-Нет главного ключа!");
        }

        for (String s : mapKeys.keySet()) {

            if (!hmCol.containsKey(s)) {

                throw new NullPointerException(nameTable + "- нет ключа " + s);
            }

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

        if (GenerateKey) {
            psInsert = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        } else {

            psInsert = connection.prepareStatement(sql);
        }

    }

    public void replaceRecInTable(Map<String, Object> hmColCurr, boolean bInsert) throws SQLException {
        int i = 1;

        Object val = null;

        for (String k : mapKeys.keySet()) {

            if (hmColCurr.containsKey(k)) {

                val = hmColCurr.get(k);
                mapKeys.put(k, val);

            } else {

                throw new SQLException("Столбца ключа '" + k + "' в таблице '" + nameTable + "' нет !");

            }

        }
        for (String namCol : hmCol.keySet()) {

            if (mapKeys.containsKey(namCol)) {
                continue;
            }

            if (hmColCurr.containsKey(namCol)) {
                val = hmColCurr.get(namCol);
            } else {
                val = null;
            }

            if (val instanceof File) {
                File file = (File) val;

                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    throw new SQLException(ex);
                }
                psReplace.setBinaryStream(i, fis, file.length());
                // fis.close();

            } else {
                psReplace.setObject(i, val);
            }
            i++;
        }

        // добавляем условие
        for (Object v : mapKeys.values()) {

            psReplace.setObject(i, v);
            i++;
        }

        int iReplace = psReplace.executeUpdate();

        // Если не обновил, значит новая запись
        if (iReplace < 1 && bInsert) {

            insertRecInTable(hmColCurr);

        }

    }

    /**
     * Вставляет новую запись в таблицу
     *
     * @param hmColCurr карта записываемых столбцов в виде имя-значение
     */
    public Integer insertRecInTable(Map<String, Object> hmColCurr) throws SQLException {

        Integer id_key = null;
        Object val = null;

        int i = 1;
        for (String namCol : hmCol.keySet()) {

            if (hmColCurr.containsKey(namCol)) {
                val = hmColCurr.get(namCol);
            } else {
                val = null;
            }

            if (val instanceof File) {
                File file = (File) val;

                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException ex) {
                    throw new SQLException(ex);
                }
                psInsert.setBinaryStream(i, fis, file.length());

            } else {
                psInsert.setObject(i, val);
            }

            i++;
        }

        psInsert.execute();

        if (GenerateKey) {
            ResultSet rs = psInsert.getGeneratedKeys();

            if (rs != null) {

                try {
                    if (rs.next()) {
                        id_key = rs.getInt(1);
                    }

                } finally {
                    rs.close();
                }
            }
        }

        return id_key;
    }
}
