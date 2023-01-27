package edu.kit.scc.webreg.dao.ops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.metamodel.SingularAttribute;

/**
 * Regapp Query Language (RQL) providing class, a DSL for searches against the
 * DB. Provides logical connectives and predicates to build expressions used in
 * queries.
 * <p/>
 * Predicates come in two flavors: With fields declared as strings and with
 * fields as JPA-metamodel attributes. The former supports chained fields (like
 * "user.givenName"), the latter does not.
 */
public class RqlExpressions {

	private RqlExpressions() {
		// Helper class
	}

	/**
	 * Provides equal predicate.
	 * 
	 * @param <E>           Type of the entity the field belongs to
	 * @param <F>           Type of the field
	 * @param field         Field attribute according to the JPA meta-model
	 * @param assignedValue Value the field is compared with
	 * @return RQL predicate {@link Equal}
	 */
	public static <E, F> Equal<E, F> equal(SingularAttribute<E, F> field, F assignedValue) {
		return new EqualBasedOnAttribute<>(field, assignedValue);
	}

	/**
	 * Provides equal predicate.
	 * 
	 * @param <E>           Type of the root entity the field is based on
	 * @param <F>           Type of the field
	 * @param field         Field name (supports chained fields)
	 * @param assignedValue Value the field is compared with
	 * @return RQL predicate {@link Equal}
	 */
	public static <E, F> Equal<E, F> equal(String field, F assignedValue) {
		return new EqualBasedOnString<>(field, assignedValue);
	}

	/**
	 * Provides not-equal predicate.
	 * 
	 * @param <E>           Type of the entity the field belongs to
	 * @param <F>           Type of the field
	 * @param field         Field attribute according to the JPA meta-model
	 * @param assignedValue Value the field is compared with
	 * @return RQL predicate {@link NotEqual}
	 */
	public static <E, F> NotEqual<E, F> notEqual(SingularAttribute<E, F> field, F assignedValue) {
		return new NotEqualBasedOnAttribute<>(field, assignedValue);
	}

	/**
	 * Provides not-equal predicate.
	 * 
	 * @param <E>           Type of the root entity the field is based on
	 * @param <F>           Type of the field
	 * @param field         Field name (supports chained fields)
	 * @param assignedValue Value the field is compared with
	 * @return RQL predicate {@link NotEqual}
	 */
	public static <E, F> NotEqual<E, F> notEqual(String field, F assignedValue) {
		return new NotEqualBasedOnString<>(field, assignedValue);
	}

	/**
	 * Convenience methode for
	 * {@link #like(SingularAttribute, String, LikeMatchMode)}.
	 * 
	 * @param <E>     Type of the entity the field belongs to
	 * @param field   Field attribute according to the JPA meta-model
	 * @param pattern Pattern that is contained by the field value
	 * @return RQL predicate {@link Like}
	 */
	public static <E> Like<E> contains(SingularAttribute<E, String> field, String pattern) {
		return like(field, pattern, LikeMatchMode.CONTAINS);
	}

	/**
	 * Provides like predicate.
	 * <p/>
	 * Works only on fields of type {@link String}. The like predicate matches a
	 * field against a given pattern ignoring case under consideration of the
	 * matching mode.
	 * 
	 * @param <E>       Type of the entity the field belongs to
	 * @param field     Field attribute according to the JPA meta-model
	 * @param pattern   Pattern the field is matched with (under consideration of
	 *                  the <code>matchMode</code>)
	 * @param matchMode Depending on the mode wildcards are placed before and/or
	 *                  after the pattern
	 * @return RQL predicate {@link Like}
	 */
	public static <E> Like<E> like(SingularAttribute<E, String> field, String pattern, LikeMatchMode matchMode) {
		return new LikeBasedOnAttribute<>(field, pattern, matchMode);
	}

	/**
	 * Convenience methode for {@link #like(String, String, LikeMatchMode)}.
	 * 
	 * @param <E>     Type of the root entity the field is based on
	 * @param field   Field name (supports chained fields)
	 * @param pattern Pattern that is contained by the field value
	 * @return RQL predicate {@link Like}
	 */
	public static <E> Like<E> contains(String field, String pattern) {
		return like(field, pattern, LikeMatchMode.CONTAINS);
	}

	/**
	 * Provides like predicate.
	 * <p/>
	 * Works only on fields of type {@link String}. The like predicate matches a
	 * field against a given pattern ignoring case under consideration of the
	 * matching mode.
	 * 
	 * @param <E>       Type of the root entity the field is based on
	 * @param field     Field name (supports chained fields)
	 * @param pattern   Pattern the field is matched with (under consideration of
	 *                  the <code>matchMode</code>)
	 * @param matchMode Depending on the mode wildcards are placed before and/or
	 *                  after the pattern
	 * @return RQL predicate {@link Like}
	 */
	public static <E> Like<E> like(String field, String pattern, LikeMatchMode matchMode) {
		return new LikeBasedOnString<>(field, pattern, matchMode);
	}

	/**
	 * Provides not-like predicate.
	 * <p/>
	 * Works only on fields of type {@link String}. The not-like predicate matches a
	 * field against a pattern using wildcards and ignoring case.
	 * 
	 * @param <E>       Type of the entity the field belongs to
	 * @param field     Field attribute according to the JPA meta-model
	 * @param pattern   Pattern the field is matched with (under consideration of
	 *                  the <code>matchMode</code>)
	 * @param matchMode Depending on the mode wildcards are placed before and/or
	 *                  after the pattern
	 * @return RQL predicate {@link NotLike}
	 */
	public static <E> NotLike<E> notLike(SingularAttribute<E, String> field, String pattern, LikeMatchMode matchMode) {
		return new NotLikeBasedOnAttribute<>(field, pattern, matchMode);
	}

	/**
	 * Provides not-like predicate.
	 * <p/>
	 * Works only on fields of type {@link String}. The not-like predicate matches a
	 * field against a pattern using wildcards and ignoring case.
	 * 
	 * @param <E>       Type of the root entity the field is based on
	 * @param field     Field name (supports chained fields)
	 * @param pattern   Pattern, the field is matched with (under consideration of
	 *                  the <code>matchMode</code>)
	 * @param matchMode Depending on the mode wildcards are placed before and/or
	 *                  after the pattern
	 * @return RQL predicate {@link NotLike}
	 */
	public static <E> NotLike<E> notLike(String field, String pattern, LikeMatchMode matchMode) {
		return new NotLikeBasedOnString<>(field, pattern, matchMode);
	}

	/**
	 * Provides an in (list) predicate.
	 * 
	 * @param <E>            Type of the entity the field belongs to
	 * @param <F>            Type of the field
	 * @param field          Field attribute according to the JPA meta-model
	 * @param assignedValues Values, the field is compared with
	 * @return RQL predicate {@link In}
	 */
	public static <E, F> In<E, F> in(SingularAttribute<E, F> field, List<F> assignedValues) {
		return new InBasedOnAttribute<>(field, assignedValues);
	}

	/**
	 * Provides an in (list) predicate.
	 * 
	 * @param <E>            Type of the entity the field belongs to
	 * @param <F>            Type of the field
	 * @param field          Field attribute according to the JPA meta-model
	 * @param assignedValues Values, the field is compared with
	 * @return RQL predicate {@link In}
	 */
	@SafeVarargs
	public static <E, F> In<E, F> in(SingularAttribute<E, F> field, F... assignedValues) {
		return new InBasedOnAttribute<>(field, Arrays.asList(assignedValues));
	}

	/**
	 * Provides an in (list) predicate.
	 * 
	 * @param <E>            Type of the root entity the field is based on
	 * @param <F>            Type of the field
	 * @param field          Field name (supports chained fields)
	 * @param assignedValues Values, the field is compared with
	 * @return RQL predicate {@link In}
	 */
	public static <E, F> In<E, F> in(String field, List<F> assignedValues) {
		return new InBasedOnString<>(field, assignedValues);
	}

	/**
	 * Provides an in (list) predicate.
	 * 
	 * @param <E>            Type of the root entity the field is based on
	 * @param <F>            Type of the field
	 * @param field          Field name (supports chained fields)
	 * @param assignedValues Values, the field is compared with
	 * @return RQL predicate {@link In}
	 */
	@SafeVarargs
	public static <E, F> In<E, F> in(String field, F... assignedValues) {
		return new InBasedOnString<>(field, Arrays.asList(assignedValues));
	}

	/**
	 * Provides isNull predicate.
	 * 
	 * @param <E>   Type of the entity the field belongs to
	 * @param field Field attribute according to the JPA meta-model
	 * @return RQL predicate {@link IsNull}
	 */
	public static <E> IsNull<E, ?> isNull(SingularAttribute<E, ?> field) {
		return new IsNullBasedOnAttribute<>(field);
	}

	/**
	 * Provides isNull predicate.
	 * 
	 * @param <E>   Type of the root entity the field is based on
	 * @param field Field name (supports chained fields)
	 * @return RQL predicate {@link IsNull}
	 */
	public static <E> IsNull<E, ?> isNull(String field) {
		return new IsNullBasedOnString<>(field);
	}

	/**
	 * Provides conjunction ("and").
	 * 
	 * @param operand      First operand
	 * @param moreOperands Second operand and following
	 * @return RQL predicate {@link And}
	 */
	public static And and(RqlExpression operand, RqlExpression... moreOperands) {
		List<RqlExpression> operands = new ArrayList<>();
		operands.add(operand);
		operands.addAll(Arrays.asList(moreOperands));
		return new And(operands);
	}

	/**
	 * Provides disjunction ("or").
	 * 
	 * @param operand      First operand
	 * @param moreOperands Second operand and following
	 * @return RQL predicate {@link And}
	 */
	public static Or or(RqlExpression operand, RqlExpression... moreOperands) {
		List<RqlExpression> operands = new ArrayList<>();
		operands.add(operand);
		operands.addAll(Arrays.asList(moreOperands));
		return new Or(operands);
	}

}