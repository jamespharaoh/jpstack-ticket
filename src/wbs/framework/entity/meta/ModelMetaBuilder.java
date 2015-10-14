package wbs.framework.entity.meta;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.BuilderFactory;

@SingletonComponent ("modelMetaBuilder")
public
class ModelMetaBuilder
	implements Builder {

	// prototype dependencies

	@Inject
	Provider<BuilderFactory> builderFactoryProvider;

	// collection dependencies

	@Inject
	@ModelMetaBuilderHandler
	Map<Class<?>,Provider<Object>> modelMetaBuilderProviders;

	// state

	Builder builder;

	// init

	@PostConstruct
	public
	void init () {

		BuilderFactory builderFactory =
			builderFactoryProvider.get ();

		for (
			Map.Entry<Class<?>,Provider<Object>> modelMetaBuilderEntry
				: modelMetaBuilderProviders.entrySet ()
		) {

			builderFactory.addBuilder (
				modelMetaBuilderEntry.getKey (),
				modelMetaBuilderEntry.getValue ());

		}

		builder =
			builderFactory.create ();

	}

	// builder

	@Override
	public
	void descend (
			Object parentObject,
			List<?> childObjects,
			Object targetObject) {

		/*
		List<Object> firstPass =
			new ArrayList<Object> ();

		List<Object> secondPass =
			new ArrayList<Object> ();

		for (
			Object childObject
				: childObjects
		) {

			if (childObject instanceof FormFieldSetSpec) {

				firstPass.add (
					childObject);

			} else {

				secondPass.add (
					childObject);

			}

		}

		builder.descend (
			parentObject,
			firstPass,
			targetObject);

		builder.descend (
			parentObject,
			secondPass,
			targetObject);

		*/

	}

}