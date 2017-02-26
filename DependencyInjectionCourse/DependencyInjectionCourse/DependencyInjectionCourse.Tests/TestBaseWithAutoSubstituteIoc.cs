using AutofacContrib.NSubstitute;

namespace DependencyInjectionCourse.Tests
{
    public abstract class TestBaseWithAutoSubstituteIoc
    {
        protected AutoSubstitute AutoSubstitute;

        protected TestBaseWithAutoSubstituteIoc()
        {
            AutoSubstitute = new AutoSubstitute();
        }

        protected T AFake<T>()
        {
            return AutoSubstitute.Resolve<T>();
        }

        protected TService Use<TService, TImplementation>()
        {
            return AutoSubstitute.Provide<TService, TImplementation>();
        }
    }
}
