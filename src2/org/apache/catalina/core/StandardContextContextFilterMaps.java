package org.apache.catalina.core;

import java.util.Arrays;

import javax.servlet.ServletContext;

import org.apache.catalina.deploy.FilterMap;

/**
 * A helper class to manage the filter mappings in a Context.
 */
public final class StandardContextContextFilterMaps {
    private final Object lock = new Object();

    /**
     * The set of filter mappings for this application, in the order they
     * were defined in the deployment descriptor with additional mappings
     * added via the {@link ServletContext} possibly both before and after
     * those defined in the deployment descriptor.
     */
    private FilterMap[] array = new FilterMap[0];

    /**
     * Filter mappings added via {@link ServletContext} may have to be
     * inserted before the mappings in the deployment descriptor but must be
     * inserted in the order the {@link ServletContext} methods are called.
     * This isn't an issue for the mappings added after the deployment
     * descriptor - they are just added to the end - but correctly the
     * adding mappings before the deployment descriptor mappings requires
     * knowing where the last 'before' mapping was added.
     */
    private int insertPoint = 0;

    /**
     * Return the set of filter mappings.
     */
    public FilterMap[] asArray() {
        synchronized (lock) {
            return array;
        }
    }

    /**
     * Add a filter mapping at the end of the current set of filter
     * mappings.
     * 
     * @param filterMap
     *            The filter mapping to be added
     */
    public void add(FilterMap filterMap) {
        synchronized (lock) {
            FilterMap results[] = Arrays.copyOf(array, array.length + 1);
            results[array.length] = filterMap;
            array = results;
        }
    }

    /**
     * Add a filter mapping before the mappings defined in the deployment
     * descriptor but after any other mappings added via this method.
     * 
     * @param filterMap
     *            The filter mapping to be added
     */
    public void addBefore(FilterMap filterMap) {
        synchronized (lock) {
            FilterMap results[] = new FilterMap[array.length + 1];
            System.arraycopy(array, 0, results, 0, insertPoint);
            System.arraycopy(array, insertPoint, results, insertPoint + 1,
                    array.length - insertPoint);
            results[insertPoint] = filterMap;
            array = results;
            insertPoint++;
        }
    }

    /**
     * Remove a filter mapping.
     *
     * @param filterMap The filter mapping to be removed
     */
    public void remove(FilterMap filterMap) {
        synchronized (lock) {
            // Make sure this filter mapping is currently present
            int n = -1;
            for (int i = 0; i < array.length; i++) {
                if (array[i] == filterMap) {
                    n = i;
                    break;
                }
            }
            if (n < 0)
                return;

            // Remove the specified filter mapping
            FilterMap results[] = new FilterMap[array.length - 1];
            System.arraycopy(array, 0, results, 0, n);
            System.arraycopy(array, n + 1, results, n, (array.length - 1)
                    - n);
            array = results;
            if (n < insertPoint) {
                insertPoint--;
            }
        }
    }
}