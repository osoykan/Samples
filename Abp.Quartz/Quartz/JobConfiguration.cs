using System.Collections.Generic;

namespace Abp.Quartz.Quartz
{
    public class JobConfiguration : IJobConfiguration
    {
        public string Group { get; set; }

        public string Name { get; set; }

        public IDictionary<string, object> JobData { get; set; }
    }
}