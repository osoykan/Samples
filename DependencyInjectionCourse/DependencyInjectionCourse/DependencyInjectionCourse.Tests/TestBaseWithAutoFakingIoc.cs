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

        protected T Fake<T>()
        {
            return FakeResolver.Resolve<T>();
        }

        protected TService The<TService, TImplementation>()
        {
            return FakeResolver.Provide<TService, TImplementation>();
        }

        public void Dispose()
        {
            FakeResolver?.Dispose();
        }
    }
}
