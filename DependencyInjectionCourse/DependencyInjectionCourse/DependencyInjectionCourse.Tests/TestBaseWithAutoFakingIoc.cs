using System;

using Autofac.Extras.FakeItEasy;

namespace DependencyInjectionCourse.Tests
{
    public abstract class TestBaseWithAutoFakingIoc : IDisposable
    {
        protected AutoFake FakeResolver;

        protected TestBaseWithAutoFakingIoc()
        {
            FakeResolver = new AutoFake();
        }

        public void Dispose()
        {
            FakeResolver?.Dispose();
        }

        protected T Fake<T>()
        {
            return FakeResolver.Resolve<T>();
        }

        protected TService The<TService, TImplementation>()
        {
            return FakeResolver.Provide<TService, TImplementation>();
        }
    }
}
