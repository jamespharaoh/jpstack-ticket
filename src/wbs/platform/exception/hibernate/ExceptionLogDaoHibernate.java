package wbs.platform.exception.hibernate;

import static wbs.framework.utils.etc.Misc.isNotNull;
import static wbs.framework.utils.etc.NumberUtils.toJavaIntegerRequired;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Instant;

import lombok.NonNull;
import wbs.framework.hibernate.HibernateDao;
import wbs.platform.exception.model.ExceptionLogDao;
import wbs.platform.exception.model.ExceptionLogRec;
import wbs.platform.exception.model.ExceptionLogSearch;

public
class ExceptionLogDaoHibernate
	extends HibernateDao
	implements ExceptionLogDao {

	@Override
	public
	Long countWithAlert () {

		return findOne (
			"countWithAlert ()",
			Long.class,

			createCriteria (
				ExceptionLogRec.class,
				"_exceptionLog")

			.add (
				Restrictions.eq (
					"_exceptionLog.alert",
					true))

			.setProjection (
				Projections.rowCount ())

		);

	}

	@Override
	public
	Long countWithAlertAndFatal () {

		return findOne (
			"countWithAlertAndFatal ()",
			Long.class,

			createCriteria (
				ExceptionLogRec.class,
				"_exceptionLog")

			.add (
				Restrictions.eq (
					"_exceptionLog.alert",
					true))

			.add (
				Restrictions.eq (
					"_exceptionLog.fatal",
					true))

			.setProjection (
				Projections.rowCount ())

		);

	}

	@Override
	public
	List <Long> searchIds (
			ExceptionLogSearch search) {

		Criteria criteria =
			createCriteria (
				ExceptionLogRec.class);

		if (
			isNotNull (
				search.timestamp ())
		) {

			criteria.add (
				Restrictions.ge (
					"timestamp",
					search.timestamp ().start ()));

			criteria.add (
				Restrictions.lt (
					"timestamp",
					search.timestamp ().end ()));

		}

		if (
			isNotNull (
				search.typeId ())
		) {

			criteria.add (
				Restrictions.eq (
					"type.id",
					search.typeId ()));

		}

		if (
			isNotNull (
				search.userId ())
		) {

			criteria.add (
				Restrictions.eq (
					"user.id",
					search.userId ()));

		}

		if (
			isNotNull (
				search.sourceContains ())
		) {

			criteria.add (
				Restrictions.ilike (
					"source",
					"%" + search.sourceContains () + "%"));

		}

		if (
			isNotNull (
				search.summaryContains ())
		) {

			criteria.add (
				Restrictions.ilike (
					"summary",
					"%" + search.summaryContains () + "%"));

		}

		if (
			isNotNull (
				search.dumpContains ())
		) {

			criteria.add (
				Restrictions.ilike (
					"dump.text",
					"%" + search.dumpContains () + "%"));

		}

		if (
			isNotNull (
				search.alert ())
		) {

			criteria.add (
				Restrictions.eq (
					"alert",
					search.alert ()));

		}

		if (
			isNotNull (
				search.fatal ())
		) {

			criteria.add (
				Restrictions.eq (
					"fatal",
					search.fatal ()));

		}

		if (
			isNotNull (
				search.resolution ())
		) {

			criteria.add (
				Restrictions.eq (
					"resolution",
					search.resolution ()));

		}

		if (search.order () != null) {

			switch (search.order ()) {

			case timestampDesc:

				criteria.addOrder (
					Order.desc (
						"timestamp"));

				break;

			default:

				throw new IllegalArgumentException ();

			}

		}

		if (search.maxResults () != null) {

			criteria.setMaxResults (
				toJavaIntegerRequired (
					search.maxResults ()));

		}

		criteria.setProjection (
			Projections.id ());

		return findMany (
			"searchIds (search)",
			Long.class,
			criteria);

	}

	@Override
	public
	List <ExceptionLogRec> findOldLimit (
			@NonNull Instant cutoffTime,
			@NonNull Long maxResults) {

		return findMany (
			"findOldLimit (cutoffTime, maxResults)",
			ExceptionLogRec.class,

			createCriteria (
				ExceptionLogRec.class,
				"_exceptionLog")

			.add (
				Restrictions.lt (
					"_exceptionLog.timestamp",
					cutoffTime))

			.addOrder (
				Order.asc (
					"_exceptionLog.timestamp"))

			.setMaxResults (
				toJavaIntegerRequired (
					maxResults))

		);

	}

}
