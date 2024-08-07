/*
 *  Copyright (C) <2024> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.bukkit.storage.method.database.sql;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.momirealms.customfishing.api.BukkitCustomFishingPlugin;
import net.momirealms.customfishing.api.storage.StorageType;
import net.momirealms.customfishing.common.dependency.Dependency;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.EnumSet;

/**
 * An implementation of AbstractSQLDatabase that uses the H2 embedded database for player data storage.
 */
public class H2Provider extends AbstractSQLDatabase {

    private Object connectionPool;
    private Method disposeMethod;
    private Method getConnectionMethod;

    public H2Provider(BukkitCustomFishingPlugin plugin) {
        super(plugin);
    }

    /**
     * Initialize the H2 database and connection pool based on the configuration.
     */
    @Override
    public void initialize(YamlDocument config) {
        File databaseFile = new File(plugin.getDataFolder(), config.getString("H2.file", "data.db"));
        super.tablePrefix = config.getString("H2.table-prefix", "customfishing");

        final String url = String.format("jdbc:h2:%s", databaseFile.getAbsolutePath());
        ClassLoader classLoader = plugin.getDependencyManager().obtainClassLoaderWith(EnumSet.of(Dependency.H2_DRIVER));
        try {
            Class<?> connectionClass = classLoader.loadClass("org.h2.jdbcx.JdbcConnectionPool");
            Method createPoolMethod = connectionClass.getMethod("create", String.class, String.class, String.class);
            this.connectionPool = createPoolMethod.invoke(null, url, "sa", "");
            this.disposeMethod = connectionClass.getMethod("dispose");
            this.getConnectionMethod = connectionClass.getMethod("getConnection");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        super.createTableIfNotExist();
    }

    @Override
    public void disable() {
        if (connectionPool != null) {
            try {
                disposeMethod.invoke(connectionPool);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.H2;
    }

    @Override
    public Connection getConnection() {
        try {
            return (Connection) getConnectionMethod.invoke(connectionPool);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
