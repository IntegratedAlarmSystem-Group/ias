package org.eso.ias.types;

import scala.AnyVal;
import scala.collection.Iterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The array of numbers is backed by a {@link ArrayList} of Numbers.
 *
 * Objects of this class checks the type of the values set in the {@link ArrayList}
 *
 * NumericArray is immutable
 */
public class NumericArray {

    /**
     * The supported types of array
     */
    public enum NumericArrayType {
        LONG,
        DOUBLE;
    }

    /**
     * The type of the items of the arrays
     */
    public final NumericArrayType numericArrayType;

    /**
     * The not null immutable array of items
     */
    public final List<? extends Number> array;

    /**
     * Constructor with no elements in the array
     *
     * @param numericArrayType The type of the items of the arrays
     */
    public NumericArray(NumericArrayType numericArrayType) {
        Objects.requireNonNull(numericArrayType,"Invalid null numeric array type");
        this.numericArrayType = numericArrayType;
        this.array = Collections.unmodifiableList(new ArrayList<>());
    }

    /**
     * Constructor with a list of elements in the array
     *
     * @param numericArrayType The type of the items of the arrays
     * @param elements the elements in the array (can be null)
     */
    public NumericArray(NumericArrayType numericArrayType, List<? extends Number> elements) {
        Objects.requireNonNull(numericArrayType,"Invalid null numeric array type");
        this.numericArrayType = numericArrayType;
        if (elements==null) {
            array = Collections.unmodifiableList(new ArrayList<>());
        } else {
            array = Collections.unmodifiableList(elements);
        }
    }

    /**
     * Constructor with a scala list of elements in the array
     *
     * @param elements the elements in the array (can be null)
     * @param numericArrayType The type of the items of the arrays
     */
    public NumericArray(scala.collection.immutable.List<? extends AnyVal> elements, NumericArrayType numericArrayType) {
        Objects.requireNonNull(numericArrayType,"Invalid null numeric array type");
        this.numericArrayType = numericArrayType;
        if (elements==null) {
            array = Collections.unmodifiableList(new ArrayList<>());
        } else {

            if (numericArrayType==NumericArrayType.DOUBLE) {
                ArrayList<Double> temp = new ArrayList<Double>();
                Iterator iter = elements.iterator();
                while (iter.hasNext()) {
                    Number valueToAdd = (Number)iter.next();
                    temp.add(valueToAdd.doubleValue());
                }
                array = Collections.unmodifiableList(temp);
            } else if (numericArrayType==NumericArrayType.LONG){
                ArrayList<Long> temp = new ArrayList<Long>();
                Iterator iter = elements.iterator();
                while (iter.hasNext()) {
                    Number valueToAdd = (Number)iter.next();
                    temp.add(valueToAdd.longValue());
                }
                array = Collections.unmodifiableList(temp);
            } else {
                throw new UnsupportedOperationException("Unsupported array type "+numericArrayType);
            }

        }
    }

    /**
     * Build and return a NumericArray with the same type and no
     * elements
     *
     * @return a new empty NumericArray
     */
    public NumericArray clear() {
        return new NumericArray(numericArrayType);
    }

    /**
     * Check if the type of the passed value matches with the type of
     * elements in the arrays
     *
     * @param value The values whose type the methods must check
     * @return true if the type of the value matches with the type of
     *              elements of the arrays; false otherwise
     */
    private boolean checkValueType(Number value) {
        switch (this.numericArrayType) {
            case DOUBLE: {
                if (value instanceof Double ||
                        value instanceof Float)
                {
                    return true;
                } else {
                    return false;
                }
            }
            case LONG: {
                if (value instanceof Long ||
                        value instanceof Integer||
                        value instanceof Byte||
                        value instanceof Short) {
                    return true;
                } else {
                    return false;
                }
            }
            default:
                throw new UnsupportedOperationException("Unrecognized array type "+this.numericArrayType);
        }
    }

    /**
     * Return a new NumericArray with the passed value appended at the end of
     * array of elements
     *
     * @param value the not null value to append to the arrays
     * @return A NumericElement with the passed value appended
     */
    public NumericArray add(Number value) {
        Objects.requireNonNull(value, "Cannot add a null value");
        if (!checkValueType(value)) {
            throw new UnsupportedOperationException("Wrong type of value "+value+" ("+numericArrayType+"expected)");
        }
        List<Number> elements = new ArrayList<>(array);
        elements.add(value);
        return new NumericArray(numericArrayType,elements);
    }

    /**
     * Return a new NumericArray with the passed value saved in the index position
     *
     * @param index The index in the arrays
     * @param value The value to set in the index position
     * @throws IndexOutOfBoundsException - if the index is out of range ({@literal index < 0 || index > size()})
     */
    public NumericArray set(int index, Number value) {
        Objects.requireNonNull(value, "Cannot add a null value");
        if (!checkValueType(value)) {
            throw new UnsupportedOperationException("Wrong type of value "+value+" ("+numericArrayType+"expected)");
        }
        List<Number> elements = new ArrayList<>(array);
        elements.add(index,value);
        return new NumericArray(numericArrayType,elements);
    }

    /**
     * Return the element at the given position of the arrays
     *
     * @param i the index of the element to get
     * @return The Number at the given position  of  the arrays
     * @see ArrayList#get(int)
     */
    public Number get(int i) {
        return array.get(i);
    }

    /**
     *
     * @return An array with all the elements in the {@link ArrayList}
     */
    public Number[] toArray() {
        int size = array.size();
        Number[] ret = new Number[size];
        return array.toArray(ret);
    }

    /**
     * Return the arrays as an array of Long
     *
     * Note that depending on the type of the array (DOUBLE for instance) the
     * numbers of the returned array differ from the original (rounded)
     *
     * @return the array of Long
     */
    public Long[] toArrayOfLong() {
        Number[] nums = toArray();
        Long ret[] = new Long[nums.length];
        for (int i=0; i<nums.length; i++) {
            ret[i]=nums[i].longValue();
        }
        return ret;
    }

    /**
     * Return the arrays as an array of Double
     *
     * @return the array sof Double
     */
    public Double[] toArrayOfDouble() {
        Number[] nums = toArray();
        Double ret[] = new Double[nums.length];
        for (int i=0; i<nums.length; i++) {
            ret[i]=nums[i].doubleValue();
        }
        return ret;
    }

    /**
     *
     * @return the number of element sin the arrays
     * @see ArrayList#size()
     */
    public int size() {
        return array.size();
    }

    /**
     *
     * @return true if the array is empty, false otherwise
     * @see ArrayList#isEmpty()
     */
    public boolean isEmpty() {
        return array.isEmpty();
    }

    /**
     * Builds ansd return a NumericArray of the given type by decoding the passed
     * string.
     *
     * The items in the String must be separated by a coma.
     *
     * @param type The type of the arrays
     * @param strValue The string with the elements of the array
     */
    public static NumericArray valueOf(NumericArrayType type, String strValue) {
        Objects.requireNonNull(type,"The type can't be NULL");
        if (strValue==null || strValue.isEmpty()) {
            throw new IllegalArgumentException("The string of elements of the array can't be null nor empty");
        }
        String cleanedStr=strValue.replace("[","").replace("]","").replace(" ","");
        String[] elements = cleanedStr.split(",");

        if (type==NumericArrayType.LONG) {
            List<Long> values = new ArrayList<>();
            for (String element: elements) {
                values.add(Long.valueOf(element));
            }
            return new NumericArray(type,values);
        } else if (type==NumericArrayType.DOUBLE) {
            List<Double> values = new ArrayList<>();
            for (String element: elements) {
                values.add(Double.valueOf(element));
            }
            return new NumericArray(type,values);
        } else {
            throw new UnsupportedOperationException("Unsupported array type "+type);
        }
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("Array of type ");
        ret.append(numericArrayType);
        ret.append(' ');
        ret.append(codeToString());
        return ret.toString();
    }

    /**
     * Return the elements as a comma separated string of values
     * @return A string with the values separated by a comma
     *
     */
    public String codeToString() {

       return array.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NumericArray that = (NumericArray) o;
        return numericArrayType == that.numericArrayType &&
                array.equals(that.array);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numericArrayType, array);
    }
}
