namespace DependencyInjectionCourse.Logger
{
    public class NullLogger : ILogger
    {
        public static readonly NullLogger Instance = new NullLogger();

        public void Log(string message)
        {
        }
    }
}
