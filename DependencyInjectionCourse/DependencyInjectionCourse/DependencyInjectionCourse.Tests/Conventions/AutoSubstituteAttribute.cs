using Ploeh.AutoFixture;
using Ploeh.AutoFixture.AutoNSubstitute;
using Ploeh.AutoFixture.Xunit2;

namespace DependencyInjectionCourse.Tests.Conventions
{
    public class AutoSubstituteAttribute : AutoDataAttribute
    {
        public AutoSubstituteAttribute()
            : base(new Fixture().Customize(new AutoConfiguredNSubstituteCustomization()))
        {
        }
    }
}
