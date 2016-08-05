using System.Collections.Generic;

namespace Abp.Quartz.Quartz
{
    public interface IJobConfiguration
    {
        string Group { get; set; }

        string Name { get; set; }

        IDictionary<string,object> JobData { get; set; }
    }
}