namespace CliApi.CommandHandling.Handlers.Dependencies
{
    public interface IMigrationStrategy
    {
        string Get();
    }

    public class CloudMigrationStrategy : IMigrationStrategy
    {
        public string Get()
        {
            return "Cloud";
        }
    }
}